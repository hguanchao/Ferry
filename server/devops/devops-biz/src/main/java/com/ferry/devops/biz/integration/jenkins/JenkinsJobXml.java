package com.ferry.devops.biz.integration.jenkins;

import com.ferry.common.core.utils.StrUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Jenkins Pipeline Job 配置 XML 辅助：安全解析、最小配置生成与 Ferry 托管字段更新。
 *
 * <p>解析统一走加固后的 DocumentBuilderFactory（禁 DOCTYPE/外部实体，防 XXE）；
 * 生成与更新均基于 DOM，避免手工拼接/字符串替换造成转义问题。
 * 更新只改动 Ferry 托管的三处（脚本、禁用位、Ferry 参数），其余配置原样保留。
 */
final class JenkinsJobXml {

    /**
     * Ferry 托管标记：写入 Job description，更新时用于验证归属。
     * 不带该标记的同名 Job 一律视为非 Ferry 管理，拒绝覆盖。
     */
    static final String MANAGED_MARKER_PREFIX = "Managed by Ferry: ";

    /** Jenkins Pipeline Job 根元素 */
    private static final String ROOT_ELEMENT = "flow-definition";

    /** 脚本节点：CpsFlowDefinition/script */
    private static final String TAG_SCRIPT = "script";

    /** GitHub 项目属性节点（GitHub 插件）：仓库主页链接 + 显示名称，纯装饰性元数据 */
    private static final String GITHUB_PROPERTY_TAG = "hudson.plugins.github.GitHubProjectProperty";
    private static final String GITHUB_URL_TAG = "projectUrl";
    private static final String GITHUB_NAME_TAG = "displayName";

    /** 禁用位节点：flow-definition/disabled */
    private static final String TAG_DISABLED = "disabled";

    /** Ferry 参数定义（name, description, defaultValue），无 STAGE_SPEC——脚本已自包含 */
    private static final List<ParamDef> FERRY_PARAMS = List.of(
            new ParamDef("APP_CODE", "应用编码", ""),
            new ParamDef("BRANCH", "分支", ""),
            new ParamDef("COMMIT_ID", "提交 ID 或引用值", ""),
            new ParamDef("REF_TYPE", "引用类型 1 Commit 2 Tag/Ref", "1"),
            new ParamDef("REF_VALUE", "原始引用值", ""),
            new ParamDef("REPO_URL", "仓库 Git 地址", ""),
            new ParamDef("SNAPSHOT", "流水线版本快照", ""),
            new ParamDef("GIT_CREDENTIALS_ID", "Jenkins 凭据 ID，可为空", ""));

    /** 旧协议/已废弃参数，同步时从托管 Job 中移除 */
    private static final List<String> LEGACY_PARAMS = List.of("STAGE_SPEC", "RUN_ID", "CALLBACK_URL", "STAGE_MAP");

    private record ParamDef(String name, String description, String defaultValue) {
    }

    private JenkinsJobXml() {
    }

    /**
     * 生成最小 Pipeline Job 配置。
     *
     * @param jobName             Job 名（同时写入归属标记）
     * @param appCode             应用编码（仅写入 description 便于人工辨认）
     * @param script              完整声明式脚本
     * @param disabled            是否禁用（待配置流水线为 true）
     * @param projectUrl          代码仓库主页地址（写入 GitHub 项目属性，空则不生成该属性）
     * @param projectDisplayName  仓库显示名称（GitHub 项目属性的显示名）
     */
    static String buildMinimalJobXml(String jobName, String appCode, String script, boolean disabled,
                                     String projectUrl, String projectDisplayName) {
        try {
            Document doc = secureFactory().newDocumentBuilder().newDocument();
            Element root = doc.createElement(ROOT_ELEMENT);
            root.setAttribute("plugin", "workflow-job");
            doc.appendChild(root);

            appendChild(root, "actions");
            appendChild(root, "description", MANAGED_MARKER_PREFIX + jobName + "（应用 " + appCode + "）");
            appendChild(root, "keepDependencies", "false");

            Element properties = appendChild(root, "properties");
            Element paramProperty = appendChild(properties, "hudson.model.ParametersDefinitionProperty");
            Element paramDefinitions = appendChild(paramProperty, "parameterDefinitions");
            for (ParamDef def : FERRY_PARAMS) {
                appendParam(paramDefinitions, def);
            }
            appendChild(properties, "org.jenkinsci.plugins.workflow.job.properties.DisableConcurrentBuildsJobProperty");
            Element discarder = appendChild(properties, "jenkins.model.BuildDiscarderProperty");
            Element strategy = appendChild(discarder, "strategy");
            strategy.setAttribute("class", "hudson.tasks.LogRotator");
            appendChild(strategy, "daysToKeep", "-1");
            appendChild(strategy, "numToKeep", "30");
            appendChild(strategy, "artifactDaysToKeep", "-1");
            appendChild(strategy, "artifactNumToKeep", "-1");
            applyGitHubProperty(properties, projectUrl, projectDisplayName);

            Element definition = appendChild(root, "definition");
            definition.setAttribute("class", "org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition");
            definition.setAttribute("plugin", "workflow-cps");
            appendChild(definition, TAG_SCRIPT, script);
            appendChild(definition, "sandbox", "true");

            appendChild(root, TAG_DISABLED, String.valueOf(disabled));
            return serialize(doc);
        } catch (Exception e) {
            throw new JenkinsDispatchException("生成 Jenkins Job 配置失败：" + e.getMessage(), false);
        }
    }

    /**
     * 解析现有 config.xml 并验证类型与归属。
     *
     * @param xml     Jenkins 返回的 config.xml
     * @param jobName 期望的 Job 名
     * @return 归属 Ferry 的配置文档；根元素不是 Pipeline Job 或缺少托管标记（含标记但 Job 名不符）返回 null
     * @throws JenkinsDispatchException XML 不合法（解析失败）
     */
    static Document loadManagedConfig(String xml, String jobName) {
        if (StrUtil.isBlank(xml)) {
            return null;
        }
        Document doc;
        try {
            doc = secureFactory().newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new JenkinsDispatchException("Jenkins Job 配置不是合法 XML：" + jobName, false);
        }
        if (!ROOT_ELEMENT.equals(doc.getDocumentElement().getTagName())) {
            return null;
        }
        String description = firstChildText(doc.getDocumentElement(), "description");
        if (description == null
                || !description.startsWith(MANAGED_MARKER_PREFIX)
                || !description.startsWith(MANAGED_MARKER_PREFIX + jobName)) {
            return null;
        }
        return doc;
    }

    /**
     * 在已验证归属的配置上更新 Ferry 托管字段：脚本、禁用位、Ferry 参数（含清理遗留 STAGE_SPEC）、
     * GitHub 项目属性。其余节点（含人工追加的属性与自定义参数）原样保留。
     */
    static String applyFerryConfig(Document doc, String script, boolean disabled,
                                   String projectUrl, String projectDisplayName) {
        Element root = doc.getDocumentElement();
        Element definition = firstChild(root, "definition");
        if (definition == null) {
            throw new JenkinsDispatchException("Jenkins Job 配置缺少脚本节点，拒绝更新", false);
        }
        setText(firstChild(definition, TAG_SCRIPT), script);

        Element disabledNode = firstChild(root, TAG_DISABLED);
        if (disabledNode == null) {
            disabledNode = appendChild(root, TAG_DISABLED);
        }
        setText(disabledNode, String.valueOf(disabled));

        Element paramDefinitions = ensureParamDefinitions(root);
        for (ParamDef def : FERRY_PARAMS) {
            if (findParam(paramDefinitions, def.name()) == null) {
                appendParam(paramDefinitions, def);
            }
        }
        NodeList stale = paramDefinitions.getElementsByTagName("name");
        for (int i = stale.getLength() - 1; i >= 0; i--) {
            Node nameNode = stale.item(i);
            if (LEGACY_PARAMS.contains(nameNode.getTextContent())) {
                Node param = nameNode.getParentNode();
                if (param != null && param.getParentNode() != null) {
                    param.getParentNode().removeChild(param);
                }
            }
        }
        applyGitHubProperty(firstChild(root, "properties"), projectUrl, projectDisplayName);
        return serialize(doc);
    }

    /**
     * 托管 GitHub 项目属性：url 非空时插入/更新（projectUrl + displayName），空时移除既有属性。
     *
     * @param properties         properties 节点（可为 null，null 时仅执行移除前的空守卫）
     * @param projectUrl         仓库主页地址，空表示不保留该属性
     * @param projectDisplayName 仓库显示名称
     */
    private static void applyGitHubProperty(Element properties, String projectUrl, String projectDisplayName) {
        if (properties == null) {
            return;
        }
        Element property = firstChild(properties, GITHUB_PROPERTY_TAG);
        if (StrUtil.isBlank(projectUrl)) {
            if (property != null) {
                properties.removeChild(property);
            }
            return;
        }
        if (property == null) {
            property = appendChild(properties, GITHUB_PROPERTY_TAG);
            appendChild(property, GITHUB_URL_TAG, projectUrl.trim());
            appendChild(property, GITHUB_NAME_TAG, StrUtil.defaultIfBlank(projectDisplayName, ""));
            return;
        }
        // 既有属性节点可能缺子节点（旧配置/手改），更新前先补齐
        Element urlNode = firstChild(property, GITHUB_URL_TAG);
        if (urlNode == null) {
            urlNode = appendChild(property, GITHUB_URL_TAG);
        }
        setText(urlNode, projectUrl.trim());
        Element nameNode = firstChild(property, GITHUB_NAME_TAG);
        if (nameNode == null) {
            nameNode = appendChild(property, GITHUB_NAME_TAG);
        }
        setText(nameNode, StrUtil.defaultIfBlank(projectDisplayName, ""));
    }

    // ==================== 内部实现 ====================

    /**
     * 加固的 DocumentBuilderFactory：禁 DOCTYPE 与外部实体，防 XXE。
     */
    private static DocumentBuilderFactory secureFactory() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory;
    }

    private static Element ensureParamDefinitions(Element root) {
        Element properties = firstChild(root, "properties");
        if (properties == null) {
            properties = root.getOwnerDocument().createElement("properties");
            root.appendChild(properties);
        }
        Element property = firstChild(properties, "hudson.model.ParametersDefinitionProperty");
        if (property == null) {
            property = properties.getOwnerDocument().createElement("hudson.model.ParametersDefinitionProperty");
            properties.appendChild(property);
        }
        Element paramDefinitions = firstChild(property, "parameterDefinitions");
        if (paramDefinitions == null) {
            paramDefinitions = property.getOwnerDocument().createElement("parameterDefinitions");
            property.appendChild(paramDefinitions);
        }
        return paramDefinitions;
    }

    private static Element findParam(Element paramDefinitions, String name) {
        NodeList params = paramDefinitions.getElementsByTagName("hudson.model.StringParameterDefinition");
        for (int i = 0; i < params.getLength(); i++) {
            Element param = (Element) params.item(i);
            if (name.equals(firstChildText(param, "name"))) {
                return param;
            }
        }
        return null;
    }

    private static void appendParam(Element paramDefinitions, ParamDef def) {
        Element param = appendChild(paramDefinitions, "hudson.model.StringParameterDefinition");
        appendChild(param, "name", def.name());
        appendChild(param, "description", def.description());
        if (def.defaultValue().isEmpty()) {
            appendChild(param, "defaultValue");
        } else {
            appendChild(param, "defaultValue", def.defaultValue());
        }
        appendChild(param, "trim", "false");
    }

    private static Element appendChild(Element parent, String tag) {
        Element child = parent.getOwnerDocument().createElement(tag);
        parent.appendChild(child);
        return child;
    }

    private static Element appendChild(Element parent, String tag, String text) {
        Element child = appendChild(parent, tag);
        child.setTextContent(text);
        return child;
    }

    private static Element firstChild(Element parent, String tag) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element element && tag.equals(element.getTagName())) {
                return element;
            }
        }
        return null;
    }

    private static String firstChildText(Element parent, String tag) {
        Element child = firstChild(parent, tag);
        return child == null ? null : child.getTextContent();
    }

    private static void setText(Element element, String text) {
        if (element == null) {
            throw new JenkinsDispatchException("Jenkins Job 配置缺少必要节点，拒绝更新", false);
        }
        element.setTextContent(text);
    }

    private static String serialize(Document doc) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            StringWriter writer = new StringWriter();
            javax.xml.transform.Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            throw new JenkinsDispatchException("序列化 Jenkins Job 配置失败：" + e.getMessage(), false);
        }
    }
}

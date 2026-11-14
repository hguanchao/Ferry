# AGENTS.md — 渡口 Ferry 全局开发规范

本文件是 Ferry 项目的**全局约束**：只写不随时间变化的约定。功能怎么实现、表里有哪些字段、某个枚举有哪些取值、
某个功能踩过什么坑，属于代码与注释的职责，不写在这里。

任何改动都必须遵守本文件；规范与代码冲突时，以本文件为准。本文件只在**约定本身变化**时才修订——
新增功能、调整配置、替换实现都不构成修改它的理由。

## 一、项目定位

渡口 Ferry 是一个**持续交付编排平台**：CI 由 Jenkins 承担，CD 支持 SSH / Docker / Kubernetes 三种部署目标。

- 后端在 `server/`，Maven 多模块，包根 `com.ferry`，按业务域拆模块，每域分 `xxx-api`（契约：entity、dto、
  query、vo、enums、feign）与 `xxx-biz`（controller、service、mapper）。
- 横切能力：`gateway`（9100，路由、鉴权、限流）、`auth`（9101，登录链路）、`common-*`（core / jdbc /
  security / web / cache）；业务服务端口自 9102 起顺延。
- 前端在 `web/`，单页应用，源码 `src/`，构建产物 `dist/` 不入库。
- 建表与种子数据、产品需求文档在 `docs/`；`docs/mysql` 只放全量建表与种子脚本，表结构变更直接改这里。
- 配置与注册中心是 Nacos：各服务通过 `spring.config.import` 拉取 `nacos:<应用名>.yaml`，网关路由也在其中；
  本地默认值写在 `application.yml` 里并用 `${ENV:默认值}` 占位。
- 数据访问只有一个 MySQL 实例，多租户按 `tenant_id` 行级隔离。

## 二、开发铁律

1. **先想清楚再写。** 动手前明确职责边界、数据流与失败路径；想不清楚就说明白，不要边写边猜。
2. **最小实现。** 只写当前需求需要的代码，不预留开关、不写无人调用的分支、不提前抽象；低耦合、高内聚。
   改动优先做**最小增量**；已能工作的代码不为了「看起来更规范」而改写。
3. **同一份规则只实现一次。** 后端能力上提到 `common-*`，前端上提到 `lib/`、`components/`；
   发现第二处复制就抽，而不是并排维护两份。
4. **失败路径必须明确。** 该拒绝就抛异常并给用户看得懂的中文提示；不静默降级、不用默认值兜底、不吞异常。
5. **安全边界只在公共层实现。** 鉴权、多租户隔离、数据权限、内部接口隔离由 `common-*` 与网关统一处理，
   业务模块不得自己发明一套，也不得把这层判断下放到前端。
6. **契约变更必须三处同步。** 后端 DTO/VO 字段、`@PreAuthorize` 标识、前端 `types.ts` / `api/*.ts`、
   菜单 `perms` 是同一次改动的一部分；只改一侧等于把问题留给运行时。
7. **不搬运他人代码。** 可借鉴设计思路，禁止整段复制；第三方源码不进入本仓库。
8. **只做被明确要求的事。** 不顺手重构无关代码、不夹带格式化、不替产品做决定；发现无关问题先说明，不擅自改。
9. **新增依赖先说明三件事**：解决什么问题、为什么现有 `common-*` / `lib/` 不够、考虑过哪些备选。
10. 所有代码注释、文档、提交信息一律使用**中文**。

## 三、注释规范

1. **单行注释**必须用一句话描述清楚这行代码是做什么的。
2. **多行注释**精简描述即可，不写长篇大论。
3. **文档注释（Javadoc）**：
   - 类：只描述类作用，如「Excel 读写工具类」「xxx 服务实现类」。
   - 方法：开头一句话描述方法作用，如「导出 Excel」；随后用 `@param` / `@return` 逐项说明参数与返回值，
     只写中文含义（如 `@param fileName 下载文件名`）；确有必要时，在开头描述换行后补一段精简的
     用途与作用说明。
   - **实体类（`entity` 包）字段例外：字段 Javadoc 必须保留**，与建表脚本的字段注释一致，
     是随代码走的数据字典，不属于「复述字段名」的删除范围。
4. 注释中**不允许出现引用文档编号或位置**（如「见 PRD 第 7 节」「见 xxx.md」）。
5. 注释中**不允许出现「参考 xx 项目」「借鉴 xx 实现」等字样**。
6. 不留注释掉的死代码，也不留 `TODO` / `FIXME` 占位；确需后续处理的，当次说清楚，而不是埋进代码。

## 四、技术栈与版本约束

### 后端

- Java 21、Spring Boot 4、Spring Cloud（Alibaba/Nacos）、MyBatis-Plus、**Jackson 3**（序列化 API 在
  `tools.jackson.*`，注解仍在 `com.fasterxml.jackson.annotation`）、Lombok、SpringDoc、Log4j2。
- 版本统一在 `server/pom.xml` 的 `dependencyManagement` 管理（含各 BOM），**业务模块不写 `<version>`**。
- `common-core` 只依赖模型与工具所需的最小集合，不引入 Web、持久层实现。
- 编译参数 `-parameters` 不得移除：Spring 靠方法参数名反射解析 `@PathVariable` / `@RequestParam`，
  缺失只会在运行期报错。

### 前端

- React、TypeScript（`strict` + `noUnusedLocals`）、Vite、antd、`@tanstack/react-query`、zustand、axios、
  react-router。
- 依赖用 `package.json` 声明并提交 `package-lock.json`；升级依赖单独提交，不夹在功能改动里。
- 当前没有 ESLint/Prettier，`npm run typecheck` 是唯一静态门禁——这不是放松要求的理由。

### 本地运行

- 构建与运行统一 **JDK 21**（`JAVA_HOME` 指向 JDK 21）；环境里另装的更低版本 JDK 只作他用。
- 启动顺序：Nacos → MySQL/Redis → 各服务 → `npm run dev`。
- 前端只请求 `/api`，由 Vite 代理到网关；网关按 `/ferry-<服务名>` 前缀 StripPrefix 后转发。
  页面里不出现网关地址与服务名。
- 配置改动先进 Nacos，再回写 `docs/` 里的样例；不在代码里写死环境地址与账号口令。

## 五、架构约束

### 依赖方向

- `gateway` 不写业务：只做路由、鉴权、限流、异常兜底；内部接口由公共层隔离，外部不可达。
- `auth` 只管登录链路，不直接访问业务表；需要用户数据走 Feign 契约。
- `xxx-api` 只放契约，不写业务逻辑，不依赖 `xxx-biz`。
- 跨模块取数走 Feign 接口，**不直连别人的表**；每个模块的 Mapper 只访问自己域内的表。
- `common-*` 不得依赖任何业务模块；公共能力只允许由业务模块单向依赖。

### 后端分层

`controller` → `service`(+`impl`) → `mapper` → 数据库，旁路是 `wrapper`（查询条件拼装 + VO 转换）。
Controller 不写业务判断，Service 不拼 HTTP 响应，Wrapper 不做业务校验。

### 前端分层

`pages`（页面与交互）/ `api`（请求定义）/ `lib`（纯函数）/ `components`（可复用展示）/ `stores`（跨页客户端状态）/
`hooks`（复用逻辑）/ `layouts`（骨架）/ `router`（路由与守卫）。页面不直接 `axios`，不手拼 URL，
不发散地写业务工具函数。

## 六、后端编码规范

### 命名与结构

| 对象 | 约定 |
| --- | --- |
| 控制器 | `XxxController`，路径为资源名 |
| 服务接口 | `I` 前缀（项目既有约定，保持一致） |
| 服务实现 | 去 `I` + `ServiceImpl` |
| 查询与转换 | `XxxWrapper` |
| 实体 / DTO / 查询 / VO | 分包存放 |
| 常量 | 按类型归入常量类（`IntConst` / `StrConst` / `SysConst` 这类） |
| 枚举 | `XxxEnum`，并提供静态校验方法 |

- 方法名沿用统一动作：`pageList` / `treeList` / `detail` / `save` / `deleted` / `authorizeXxx`。

### 接口约定

- 统一返回 `R<T>`：列表 `R<IPage<XxxVO>>` 或 `R<List<XxxTree>>`，写操作 `R<String>`，
  文案按「新增成功 / 修改成功 / 删除成功」。
- **每个接口都要显式决定是否鉴权**：需要就标 `@PreAuthorize("模块:资源:动作")`，动作取方法名，
  必须与前端 `can()` 的判断串、菜单表 `perms` 同一口径；菜单按页面给通配，按钮给具体动作。
  不标注等于不鉴权，属于评审必查项。
- 前端纯客户端动作没有后端接口，权限串仍要落在某个菜单通配里，否则非超管永远看不到入口。
- 确实要免登录的接口，才允许不标 `@PreAuthorize`：必须同时写进 `AuthConst.DEFAULT_EXCLUDE_PATH`，
  并在该路径的登记行注释里写明「为什么不鉴权」（`/tenant/list` 是范例）；漏了任一项都属于评审必查项。
- Controller 层不写 Javadoc：接口说明放对应的 IService 接口，Swagger 描述用 `@Operation` 注解承载。
- 写操作入参用 `@RequestBody @Valid`，校验注解写在 DTO 上、消息用中文；查询类入参都是可选条件，
  不加必填校验。Controller 不重复写 if 判空。
- 异常交给全局异常处理器，Controller 不 try-catch 后自行拼装错误响应。

### 业务与数据

- 业务失败抛 `ServiceException`（中文、面向用户、说清哪条规则不满足）；越权抛
  `AuthException(HttpStatus.FORBIDDEN)`。禁止用返回值 `null` / `false` 表达业务失败——
  `boolean` 只承载「数据库是否写入成功」。
- Service 继承 `com.ferry.common.jdbc.base.ServiceImpl`（**不是** MyBatis-Plus 原生 `ServiceImpl`），
  实体继承 `BaseEntity`；没有公共字段的关系表实体不继承 `BaseEntity`，其删除是物理删除，
  手写 SQL 不要加 `is_deleted` / `tenant_id` 条件。
- `create_user` / `create_time` / `update_user` / `update_time` / `tenant_id` / `is_deleted` 由填充器与
  逻辑删除统一维护，业务代码不手动赋值、不手动拼条件。
- **写侧 DTO 里不放 `tenantId`**：字段不存在才不会漏进来，比复制后置空可靠；租户由填充器按登录态写入。
  唯一性校验不要手写 `tenant_id`（拦截器已追加，重复写会让校验恒查不到重复数据）。
- 判断「一批 id 是否有关联数据」只发一条聚合查询：用 `in` 批量 `count`，不要逐个 id 查库。
- 禁止在循环里逐条查库：先 `in` 批量查、再在内存里按 id 分组。
- 查询条件统一写在 `XxxWrapper#lambda(query)`；分页用 `Page.create(body)`；禁止在 Controller 里拼 `Wrappers`。
- 实体与 VO 转换用 `BeanUtil.copy`，分页转换用 `wrapper.pageVo`；不手写逐字段 set。
- 写操作加 `@Transactional(rollbackFor = Exception.class)`，只读不加。
- 多租户 `tenant_id` 由 `TenantLineInnerInterceptor` 统一追加，业务 SQL 不重复写；数据权限由
  `DataPermissionHandler` 统一注入，需要按部门过滤的查询不要自己加 `dept_id` 条件。
- 令牌相关操作走 `RedisUtil` 封装，不直接用 `RedisTemplate`；改密、禁用、删除用户这类登录态变更
  必须调用 `AuthUtil.revokeAll`，这是安全边界，不能省。

### 树形数据

- 根节点的 `parentId` 约定为 `0`（`SysConst.TOP_PARENT_ID`）；`null` 与 `0` 都表示「没有上级」。
- 根判定**只允许**用 `TreeUtil.isRoot`：禁止 `getById(0)`，禁止自己写 `== 0L` 这类分支。
- 树构建统一用 `TreeUtil.build`，业务里不手写递归；前端对应能力在 `lib/tree.ts`，不要各页面自己再写一遍。
- 新增/编辑必须显式处理根：根节点跳过父级存在性校验，`parentId` 归一为 `0`；个别树若约定
  「`null` 表示本次不改父级」，以该功能代码注释为准。
- 父级校验只在**非根**时执行，报错文案说清是哪条上级不存在；同时必须排除「上级是自身或自身的后代」，
  否则树里会出现环、整棵子树从根上脱节；只挡「上级 = 自身」不算防环。
- 移动节点时，带路径字段的表必须同步重算全部后代的路径，只改当前节点会让按路径推导的范围停在旧位置。
- 删除树节点前先查一次直接下级并拦住，否则下级会从树/懒加载里脱节变成孤儿；
  级联只清一层时，必须先由写入侧保证层级。

### 序列化

- Long 与 BigDecimal 全局序列化为字符串，避免前端丢精度。**前端拿到的 id 一律是 string**，
  后端不要为了「看起来是数字」去改序列化配置。
- 日期格式统一用 `DateUtil` 里的常量，不在注解里写死格式串。
- VO 里不放 `password` 这类敏感字段：字段不存在才不会漏出去，不靠 Service 里逐个 `setXxx(null)` 兜底。

### SQL 与表结构

- 优先用 MyBatis-Plus 的 lambda 查询；XML 只写表达不了的（多表、子查询、聚合、复杂排序）。
- 禁止 `SELECT *`：resultMap 与列清单显式列出字段。
- 手写 SQL 必须自己带 `is_deleted = 0`（逻辑删除只作用于框架生成的 SQL）；租户条件由拦截器补，不要重复写。
- 改查询前先打开对应 Mapper/XML 看清实际 `WHERE`：过滤条件能下推到 SQL 就不要全表捞出后在内存里筛。
- 新增表按域用统一前缀、`bigint` 主键、公共字段齐全，字段与索引都要有注释；建表脚本归档在 `docs/mysql`。
  表结构变更直接改全量建表脚本并归档，不引入迁移框架。

## 七、前端编码规范

### 命名与结构

| 对象 | 约定 |
| --- | --- |
| 页面 | PascalCase，默认导出同名组件 |
| 组件 / Hook / 工具 | PascalCase / `useXxx` / camelCase |
| 接口定义 | 按域成文件，导出 `xxxApi` 对象 |
| 类型 | 集中在 `api/types.ts` |
| 引入路径 | 一律用 `@/` 别名 |

- 样式类统一 `ferry-` 前缀，块与元素用 `__` 连接；设计变量集中在 `tokens.css`。

### 状态与数据

- 服务端状态一律走 react-query：`queryKey` 用 `[资源, ...查询条件]`；写操作成功后 `invalidateQueries`
  失效相关 key，**不要**手动 `setQueryData` 复制后端数据。
- 客户端状态用 zustand；不引入 redux / mobx。
- 请求只经 `api/client.ts` 收口：鉴权头、错误归一、提示去重都在那一层，页面不重复实现。
- 错误已由拦截器统一提示，页面只在需要内联展示时传 `silent`；不要既 `silent` 又自己弹一次。
- 权限判断用 `useAuthStore((state) => state.can)('模块:资源:动作')`，字符串必须与后端 `@PreAuthorize`
  一致；通配匹配规则已由 `lib/perm.ts` 收口，页面不自己拆权限串。
- **id 一律按 string 处理**，禁止 `Number(id)`、禁止用作算术。

### 组件与表单

- 表单用 antd `Form` + `Form.Item`，提交前 `validateFields`；表格列用 `TableColumnsType`，不写 `any`。
- 页面骨架沿用 `PageHeader` + `ferry-panel` / `ferry-toolbar` / `ferry-table-wrap`，不另起一套布局。
- 反馈统一：成功 `message.success`，失败 `showError`；不直接 `message.error`，不用 `alert` / `console`
  代替用户提示。
- 禁止 `any`：外部数据先 `unknown` 再收窄；组件 props 写显式接口。
- 不写魔法值：颜色、圆角、行高、密度走 CSS 变量或 antd token。

### 界面

- 沿用现有视觉语言：浅色、克制、信息密度优先；不擅自改主题色、圆角、字号与间距体系。
- 图标统一 `lucide-react`，尺寸按上下文取 14（表格行内）/ 15（按钮）/ 17-18（顶栏）；
  界面文案与代码里不出现表情符号。
- 同一类交互只做一种：新增用右上主按钮、行内操作用 link 按钮、危险操作用 `danger` + 二次确认。

## 八、代码质量管理

### 三道门禁（每次改完自己跑，不通过不算完成）

1. **编译**：后端 `mvn -o -pl <改动模块> -am compile`（涉及公共模块时把 `-pl` 一起带上），无新增编译告警。
2. **类型与构建**：前端 `npm run typecheck` 与 `npm run build` 必须同时通过。
3. **契约同步**：DTO/VO 字段、`@PreAuthorize` 串、`types.ts` / `api/*.ts`、菜单 `perms` 逐项核对，一处不落。

### 测试要求（不写单元测试）

- **后端不写单元测试**：不引入 JUnit 等测试框架，不添加 test 依赖，不建 `src/test` 目录；
  纯函数与带分支的规则统一靠编译门禁、冒烟脚本与实际操作验证兜底。
- **前端纯函数要能被验证**：`lib/` 里的逻辑优先做成无副作用函数，用 node 脚本直接断言，不引入测试框架。
- **行为改动给出可复现验证**：接口层用冒烟脚本跑，页面/交互层记录实际点击路径与预期结果；
  验证脚本要写清「复现场景 / 断言 / 用法」，跑完不在仓库留临时文件。
- 修 bug 的验证必须能**先复现原问题**再证明已修复，只验证「新逻辑能跑」不算。

### 验证深度（按最小验证执行）

- **默认只做最小验证**：编译/类型检查通过 + 覆盖本次改动关键分支的一条断言即可，不做端到端全链路演练。
- **不自行搭建重型验证环境**：不启中间件、不拉起完整服务、不驱动浏览器、不建临时工程去复刻运行时。
  确实需要时先说明理由并征得同意。
- **验证完即清理**：临时脚本、临时工程、样本文件、下载产物不进仓库也不留在工作区。
- 完整的启动冒烟、集成检查、页面点击验证由用户自行执行；交付时如实说明「已验到什么程度、哪些没验」，
  不用「应该没问题」代替结论。

### 缺陷修复纪律

1. **先定位根因**：读完整错误信息与调用栈，确认是哪一层的问题，不猜着改。
2. **一次性修好**：找到原因后按根因修，不做「试试这个、试试那个」的补丁式改动。
3. **修完重新验证**：确认问题消失且没有连带破坏；验证方式按第八节「测试要求」执行。
4. **三次失败就停**：同一个问题连续三次没解决，停下来把已排查的结论、试过的方案、当前判断讲清楚，请求协助。
5. **不隐瞒**：构建不过、脚本报错、验证没做，都如实说明，不用「应该没问题」代替结论。

### 依赖与技术债

- 升级依赖、替换库、改构建配置，一律单独提交，并在提交信息里说明原因与影响面。
- 修复用的临时兜底不允许留在主干：要么当次补成正式实现，要么明确记录待办并说明触发条件。
- 重构与功能改动分开；重构只在不改变外部行为的前提下进行，改完必须重跑第八节的三道门禁。

## 九、Git 提交规范

采用 **Conventional Commits**：`type(scope): description`

- `description` 用**中文**，说清改了什么、为什么；如 `fix(menu): 修复根菜单保存时的父级校验误报`。
- `type`：`feat` / `fix` / `refactor` / `docs` / `chore` / `perf` / `style` / `build` / `ci`。
- `scope` 用模块或页面名。
- 单次提交聚焦一件事；格式化与依赖升级必须单独提交。`target` / `dist` / `node_modules` / `logs` /
  `.idea` 等本地产物不进提交。

## 十、完成定义（DoD）

一个改动只有满足以下全部条件才算完成：

1. 后端 `mvn -o -pl <改动模块> -am compile` 通过（`JAVA_HOME` 为 JDK 21），无新增编译告警；
2. 前端 `npm run typecheck` 与 `npm run build` 通过；
3. 契约同步：后端 DTO/VO 字段、`@PreAuthorize` 标识、前端 `types.ts` / `api/*.ts`、菜单 `perms` 一处不落；
4. 行为改动给出可复现的验证，验证方式与深度按第八节执行；
5. 注释符合第三节，命名与位置符合第五、六、七节；
6. 按第九节提交，工作区干净。

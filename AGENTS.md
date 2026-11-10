# AGENTS.md — 渡口 Ferry 全局开发规范

本文件是 Ferry 项目的**全局约束**：只写不随时间变化的约定。功能怎么实现、表里有哪些字段、某个枚举有哪些取值，
属于代码与注释的职责，不写在这里。

任何改动都必须遵守本文件；规范与代码冲突时，以本文件为准。本文件只在**约定本身变化**时才修订——
新增功能、调整配置、替换实现都不构成修改它的理由。

## 一、项目定位

渡口 Ferry 是一个**持续交付编排平台**：CI 由 Jenkins 承担，CD 支持 SSH / Docker / Kubernetes 三种部署目标。
当前仓库是平台底座——登录鉴权、网关与系统管理域（用户、角色、菜单、部门、字典、区域、参数、租户）。

- 后端在 `server/`，Maven 多模块，包根 `com.ferry`：

  | 模块 | 职责 | 端口 |
  | --- | --- | --- |
  | `gateway` | 路由、鉴权、限流等横切能力 | 9100 |
  | `auth` | 登录、验证码、令牌签发与注销 | 9101 |
  | `system/system-api` | 系统管理域的契约：实体、DTO、查询、VO、Feign 接口 | — |
  | `system/system-biz` | 系统管理域的实现：控制器、服务、Mapper | 9102 |
  | `common/common-core` | 与框架无关的模型、工具、常量、异常、统一响应 | — |
  | `common/common-jdbc` | MyBatis-Plus 扩展：租户、数据权限、公共字段填充、分页 | — |
  | `common/common-security` | 鉴权注解与上下文、令牌工具 | — |
  | `common/common-web` | Web 通用：Jackson 配置、全局异常处理 | — |
  | `common/common-cache` | Redis 封装 | — |

- 前端在 `web/`，单页应用，源码 `src/`，构建产物 `dist/` 不入库；页面骨架、设计变量、请求层都在这一层收口。
- 建表与种子数据、产品需求文档在 `docs/`；`docs/mysql` 只放全量建表与种子脚本，日常表结构变更走 Flyway。
- 配置与注册中心是 Nacos：各服务通过 `spring.config.import` 拉取 `nacos:<应用名>.yaml`，网关路由也在其中；
  本地默认值写在 `application.yml` 里并用 `${ENV:默认值}` 占位。
- 数据访问只有一个 MySQL 实例，多租户按 `tenant_id` 行级隔离；`system` 域的表结构由 `system` 服务自己迁移。

## 二、开发铁律

1. **先想清楚再写。** 动手前明确职责边界、数据流与失败路径；想不清楚就说明白，不要边写边猜。
2. **最小实现。** 只写当前需求需要的代码，不预留开关、不写无人调用的分支、不提前抽象；低耦合、高内聚。
   改动优先做**最小增量**：能并进一个条件判断的不新增分支，能一次 SQL 解决的不加辅助方法，
   能一行补全的不重排整个方法；已能工作的代码不为了「看起来更规范」而改写。
3. **同一份规则只实现一次。** 后端能力上提到 `common-*`，前端上提到 `lib/`、`components/`；发现第二处复制就抽，
   而不是并排维护两份（树的根判定、错误提示去重、统一响应都是这么收敛的）。
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

1. 注释说明**为什么**，不复述代码在做什么；能靠命名表达的就不写注释。
2. 注释中**不允许出现引用文档编号或位置**（如「见 PRD 第 7 节」「见 xxx.md」）。
3. 注释中**不允许出现「参考 xx 项目」「借鉴 xx 实现」等字样**。
4. 对外方法（Service 接口、工具类公共方法）写 Javadoc，包含 `@param` / `@return`；实现内部只在
   非显而易见处（边界条件、反直觉写法、踩过的坑）写行注释。
5. 踩坑修复必须留下「为什么这么写」的注释，避免后来者按直觉改回去
   （`DeptServiceImpl#save` 对 `ancestors` 用空串而非 `null` 就是这类）。
6. 不留注释掉的死代码，也不留 `TODO` / `FIXME` 占位；确需后续处理的，当次说清楚，而不是埋进代码。

## 四、技术栈与版本约束

### 后端

- Java 21、Spring Boot 4.0.9、Spring Cloud 2025.1.4（Alibaba/Nacos 2025.1.0.0）、MyBatis-Plus 3.5.18、
  **Jackson 3**（序列化 API 在 `tools.jackson.*`，注解仍在 `com.fasterxml.jackson.annotation`）、
  Lombok、SpringDoc、Flyway、Log4j2（已排除 `spring-boot-starter-logging`）。
- 版本统一在 `server/pom.xml` 的 `dependencyManagement` 管理（含各 BOM），**业务模块不写 `<version>`**。
- `common-core` 只依赖模型与工具所需的最小集合，不引入 Web、持久层实现。
- 编译参数 `-parameters` 不得移除：Spring 7 靠方法参数名反射解析 `@PathVariable` / `@RequestParam`，
  缺失只会在运行期报错。

### 前端

- React 19、TypeScript 5.9（`strict` + `noUnusedLocals`）、Vite 8、antd 6、`@tanstack/react-query` 5、
  zustand 5、axios、react-router 7、dayjs。
- 依赖用 `package.json` 声明并提交 `package-lock.json`；升级依赖单独提交，不夹在功能改动里。
- 当前没有 ESLint/Prettier，`npm run typecheck` 是唯一静态门禁——这不是放松要求的理由。

### 本地运行

- 构建与运行统一 **JDK 21**（`JAVA_HOME` 指向 JDK 21）；环境里另装的 JDK 8/17 只作他用，
  用错版本会直接报 `release 21 not supported`。
- 启动顺序：Nacos → MySQL/Redis → `gateway` / `auth` / `system` → `npm run dev`（5173）。
- 前端只请求 `/api`，由 Vite 代理到网关（`VITE_GATEWAY`，默认 `http://127.0.0.1:9100`）；
  网关按 `/ferry-<服务名>` 前缀 StripPrefix 后转发。页面里不出现网关地址与服务名。
- 配置改动先进 Nacos，再回写 `docs/` 里的样例；不在代码里写死环境地址与账号口令。

## 五、架构约束

### 依赖方向

- `gateway` 不写业务：只做路由、鉴权、限流、异常兜底；内部接口由 `InternalPathFilter` 隔离，外部不可达。
- `auth` 只管登录链路，不直接访问 `system` 的业务表；需要用户数据走 Feign 契约。
- `system-api` 只放契约（entity / dto / query / vo / enums / feign），不写业务逻辑，不依赖 `system-biz`。
- 跨模块取数走 `system-api` 里的 Feign 接口，**不直连别人的表**；每个模块的 Mapper 只访问自己域内的表。
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

| 对象 | 约定 | 示例 |
| --- | --- | --- |
| 控制器 | `XxxController`，路径为资源名 | `DeptController` / `/dept` |
| 服务接口 | `I` 前缀（项目既有约定，保持一致） | `IDeptService` |
| 服务实现 | 去 `I` + `ServiceImpl` | `DeptServiceImpl` |
| 查询与转换 | `XxxWrapper` | `DeptWrapper#lambda` |
| 实体 / DTO / 查询 / VO | 分包存放 | `api/entity`、`api/dto`、`api/dto/query`、`api/vo`、`api/vo/tree` |
| 常量 | 按类型归入常量类 | `IntConst` / `StrConst` / `SysConst` |
| 枚举 | `XxxEnum`，并提供静态校验方法 | `MenuTypeEnum#contains` |

- 方法名沿用统一动作：`pageList` / `treeList` / `detail` / `save` / `deleted` / `authorizeXxx`。

### 接口约定

- 统一返回 `R<T>`：列表 `R<IPage<XxxVO>>` 或 `R<List<XxxTree>>`，写操作 `R<String>`，
  文案按「新增成功 / 修改成功 / 删除成功」。
- **每个接口都要显式决定是否鉴权**：需要就标 `@PreAuthorize("模块:资源:动作")`，动作取方法名
  （`system:dept:pageList`、`system:dept:deleted`），必须与前端 `can()` 的判断串、菜单表 `perms` 同一口径；
  菜单按页面给通配 `system:dept:*`，按钮给具体动作。不标注等于不鉴权，属于评审必查项。
- 前端纯客户端动作（如导出 CSV）没有后端接口，权限串仍要落在某个菜单通配里，否则非超管永远看不到入口。
- 确实要免登录的接口，才允许不标 `@PreAuthorize`：必须同时写进 `AuthConst.DEFAULT_EXCLUDE_PATH`，
  并在方法上留下「为什么不鉴权」的注释（`/tenant/list` 是范例）；漏了任一项都属于评审必查项。
- 写操作入参用 `@RequestBody @Valid`，校验注解写在 DTO 上、消息用中文；查询类入参都是可选条件，
  不加必填校验。Controller 不重复写 if 判空。
- 异常交给全局 `ExceptionAdviceHandler`，Controller 不 try-catch 后自行拼装错误响应。

### 业务与数据

- 业务失败抛 `ServiceException`（中文、面向用户、说清哪条规则不满足）；越权抛 `AuthException(HttpStatus.FORBIDDEN)`。
  禁止用返回值 `null` / `false` 表达业务失败——`boolean` 只承载「数据库是否写入成功」。
- Service 继承 `com.ferry.common.jdbc.base.ServiceImpl`（**不是** MyBatis-Plus 原生 `ServiceImpl`），
  实体继承 `BaseEntity`。
- `create_user` / `create_time` / `update_user` / `update_time` / `tenant_id` / `is_deleted` 由填充器与
  逻辑删除统一维护，业务代码不手动赋值、不手动拼 `is_deleted` 条件。
- **写侧 DTO 里不放 `tenantId`**：字段不存在才不会漏进来，比复制后置空可靠；租户由填充器按登录态写入。
  唯一性校验不要手写 `tenant_id`（拦截器已追加，重复写会相与，让校验恒查不到重复数据）。
  Jackson 已关闭 `FAIL_ON_UNKNOWN_PROPERTIES`，旧请求里仍带该字段会被忽略而不是报错。
- 判断「一批 id 是否有关联数据」只发一条聚合查询：用 `in` 批量 `count`，不要逐个 id 查库
  （`DeptMapper#selectUserCountByIds`、`PostMapper#selectUserCountByIds` 是范例）。
- `sys_user_role` / `sys_role_menu` / `sys_role_dept` 没有公共字段，实体不继承 `BaseEntity`：
  它们的 `remove` 是物理删除，手写 SQL 不要给这几张表加 `is_deleted` / `tenant_id` 条件。
- 查询条件统一写在 `XxxWrapper#lambda(query)`；分页用 `Page.create(body)`；禁止在 Controller 里拼 `Wrappers`。
- 实体与 VO 转换用 `BeanUtil.copy`，分页转换用 `wrapper.pageVo`；不手写逐字段 set。
- 写操作加 `@Transactional(rollbackFor = Exception.class)`，只读不加。
- 禁止在循环里逐条查库：先 `in` 批量查、再在内存里按 id 分组（`UserServiceImpl#pageList` 是范例）。
- 多租户 `tenant_id` 由 `TenantLineInnerInterceptor` 统一追加，业务 SQL 不重复写；数据权限由
  `DataPermissionHandler` 统一注入，需要按部门过滤的查询不要自己加 `dept_id` 条件。
- 令牌与登录失败计数走 `RedisUtil` 封装，不直接用 `RedisTemplate`；改密、禁用、删除用户必须调用
  `AuthUtil.revokeAll`，这是安全边界，不能省。

### 树形数据（本项目已踩过的坑，按此执行）

- 根节点的 `parentId` 约定为 `0`（`SysConst.TOP_PARENT_ID`）；`null` 与 `0` 都表示「没有上级」。
- 根判定**只允许**用 `TreeUtil.isRoot`：禁止 `getById(0)`，禁止自己写 `== 0L`、`!TOP_PARENT_ID.equals(...)` 这类分支。
- 树构建统一用 `TreeUtil.build`，业务里不手写递归；前端对应能力在 `lib/tree.ts`，不要各页面自己再写一遍。
- 新增/编辑时必须显式处理根：根节点跳过父级存在性校验；带路径字段的（如部门 `ancestors`）同步重算，
  不能只改 `parentId`。部门、字典、菜单的表单始终传 `parentId`，根节点要归一成 `0`；
  **区域例外**：`null` 表示「本次不改父级」，归一成 `0` 会把区县悄悄提到省级，这是踩过的坑，
  别按其它树的直觉改回去。
- 父级校验只在**非根**时执行，报错文案说清是哪条上级不存在；同时必须排除「上级是自身或自身的后代」，
  否则树里会出现环、整棵子树从根上脱节。有路径字段的复用 `selectSelfAndChildIds` 一次查完；
  **无路径字段的（菜单）** 用一次只取 `id, parent_id` 两列的查询建父链映射，同时判上级存在，
  再沿父链上溯判断是否绕回自身（父链判定 SQL 表达不了，不算把过滤条件搬到内存），
  用已访问集合防住库里已有的环；只挡「上级 = 自身」不算防环。
- 移动节点时，带路径字段的要用一条 `UPDATE ... REPLACE(ancestors, 旧前缀, 新前缀)` 同步改掉全部后代，
  只改当前节点会让按路径算出的可见部门范围停在旧位置。
- 删除树节点前统一用 `in(parent_id, ids)` 查一次直接下级并拦住（部门、菜单、区域），
  否则下级会从树/懒加载里脱节变成孤儿；级联只清一层时，必须先由写入侧保证只有两层。
- **字典固定两层**（分组 → 字典项）：`save` 在写入侧拦住「父级不是根分组」，
  `deleted` 用 `in(parent_id, ids)` 一条语句连带清掉字典项，不做全表递归，也不把整张表捞进内存。
  写入侧保证层级，删除侧才能只清一层；分组改 `code` 时用一条带 `ne(code, 新码)` 的 `lambdaUpdate`
  同步改掉子项编码，否则按编码查询会漏掉仍挂旧码的字典项。

### 序列化

- Long 与 BigDecimal 由 `JacksonConfiguration` 全局序列化为字符串，避免前端丢精度。
  **前端拿到的 id 一律是 string**，后端不要为了「看起来是数字」去改序列化配置。
- 日期格式统一用 `DateUtil` 里的常量，不在注解里写死格式串。
- VO 里不放 `password` 这类敏感字段：字段不存在才不会漏出去，不靠 Service 里逐个 `setXxx(null)` 兜底。

### SQL 与迁移

- 优先用 MyBatis-Plus 的 lambda 查询；XML 只写表达不了的（多表、子查询、聚合、复杂排序）。
- 禁止 `SELECT *`：resultMap 与列清单显式列出字段。
- 手写 SQL 必须自己带 `is_deleted = 0`（逻辑删除只作用于框架生成的 SQL）；租户条件由拦截器补，不要重复写。
- 改查询前先打开对应 Mapper/XML 看清实际 `WHERE`：过滤条件能下推到 SQL 就不要全表捞出后在内存里筛。
- 新增表：`sys_` 前缀、`bigint` 主键、公共字段齐全，字段与索引都要有注释；建表脚本归档在 `docs/mysql`。
  `system` 服务已接入 Flyway（`classpath:db/migration`，历史表 `flyway_system_history`），
  后续表结构变更以 `V{序号}__说明.sql` 追加，不手改线上库；脚本必须可重复执行或在基线内自洽。

## 七、前端编码规范

### 命名与结构

| 对象 | 约定 | 示例 |
| --- | --- | --- |
| 页面 | PascalCase，默认导出同名组件 | `pages/system/Dept.tsx` |
| 组件 / Hook / 工具 | PascalCase / `useXxx` / camelCase | `PageHeader.tsx`、`useAutoExpandRows.ts`、`tree.ts` |
| 接口定义 | 按域成文件，导出 `xxxApi` 对象 | `api/system.ts` 的 `deptApi` |
| 类型 | 集中在 `api/types.ts` | `DeptNode`、`UserRow`、`ApiPage<T>` |
| 引入路径 | 一律用 `@/` 别名 | `import { deptApi } from '@/api/system'` |

- 样式类统一 `ferry-` 前缀，块与元素用 `__` 连接（`ferry-page-head__title`）；设计变量集中在 `tokens.css`。

### 状态与数据

- 服务端状态一律走 react-query：`queryKey` 用 `[资源, ...查询条件]`；写操作成功后 `invalidateQueries`
  失效相关 key，**不要**手动 `setQueryData` 复制后端数据。
- 客户端状态用 zustand（`auth`、`ui`）；不引入 redux / mobx。
- 请求只经 `api/client.ts` 的 `get` / `post`：鉴权头、错误归一、提示去重都在那一层，页面不重复实现。
- 错误已由拦截器统一提示，页面只在需要内联展示时传 `silent`；不要既 `silent` 又自己弹一次。
- 权限判断用 `useAuthStore((state) => state.can)('system:dept:save')`，字符串必须与后端 `@PreAuthorize` 一致；
  通配匹配规则已由 `lib/perm.ts` 收口，页面不自己拆权限串。
- **id 一律按 string 处理**，禁止 `Number(id)`、禁止用作算术。

### 组件与表单

- 表单用 antd `Form` + `Form.Item`，提交前 `validateFields`；表格列用 `TableColumnsType`，不写 `any`。
- 页面骨架沿用 `PageHeader` + `ferry-panel` / `ferry-toolbar` / `ferry-table-wrap`，不另起一套布局。
- 反馈统一：成功 `message.success`，失败 `showError`（`lib/notify.ts` 已做并发去重）；不直接 `message.error`，
  不用 `alert` / `console` 代替用户提示。
- 禁止 `any`：外部数据先 `unknown` 再收窄；组件 props 写显式接口。
- 不写魔法值：颜色、圆角、行高、树密度走 CSS 变量、antd token（`theme.ts`）或 `tokens.css`。

### 界面

- 沿用现有视觉语言：浅色、克制、信息密度优先；不擅自改主题色、圆角、字号与间距体系。
- 图标统一 `@ant-design/icons`；界面文案与代码里不出现表情符号。
- 同一类交互只做一种：新增用右上主按钮、行内操作用 link 按钮、危险操作用 `danger` + 二次确认。

## 八、代码质量管理

### 三道门禁（每次改完自己跑，不通过不算完成）

1. **编译**：后端 `mvn -o -pl <改动模块> -am compile`（涉及公共模块时把 `-pl` 一起带上），无新增编译告警。
2. **类型与构建**：前端 `npm run typecheck` 与 `npm run build` 必须同时通过——`typecheck` 挡类型，
   `build` 挡别名、资源与打包期问题。
3. **契约同步**：DTO/VO 字段、`@PreAuthorize` 串、`types.ts` / `api/*.ts`、菜单 `perms` 逐项核对，一处不落。

### 测试要求

- **必须补单测**：`common-core` 的工具类与纯函数（`HttpUtil`、`NumUtil`、`TreeUtil`、`BeanUtil` 这类），
  以及带分支规则的服务方法（根节点归一、父子校验、唯一性、越权）。测试类与被测类同包同名加 `Test`，
  放 `src/test/java` 下镜像目录。
- **不写形式化测试**：纯 CRUD 转发、getter/setter 级别的代码不硬凑覆盖率；测试要能指出「改坏了哪条规则」。
- **前端纯函数同样要能被验证**：`lib/` 里的逻辑（`tree.ts`、`perm.ts`、`export.ts`）优先做成无副作用函数，
  用 `web/scripts` 下的 node 脚本直接断言，不引入测试框架。
- **行为改动给出可复现验证**：接口层用 `web/scripts/smoke-*.mjs` 跑（`mock-gateway.mjs` 提供假后端），
  页面/交互层记录实际点击路径与预期结果。验证脚本要写清「复现场景 / 断言 / 用法」，跑完不在仓库留临时文件。
- 修 bug 的验证必须能**先复现原问题**再证明已修复，只验证「新逻辑能跑」不算。

### 缺陷修复纪律

1. **先定位根因**：读完整错误信息与调用栈，确认是哪一层的问题，不猜着改。
2. **一次性修好**：找到原因后按根因修，不做「试试这个、试试那个」的补丁式改动。
3. **修完重新验证**：确认问题消失且没有连带破坏；顺带能覆盖的分支补进测试。
4. **三次失败就停**：同一个问题连续三次没解决，停下来把已排查的结论、试过的方案、当前判断讲清楚，请求协助。
5. **不隐瞒**：构建不过、脚本报错、验证没做，都如实说明，不用「应该没问题」代替结论。

### 评审自查（提交前逐条过）

- 职责是否放对了层：Controller 有没有业务判断，Service 有没有拼响应，页面有没有直接发请求。
- 规则是否只实现了一份：同类逻辑是否在第二个地方又写了一遍。
- 失败路径是否明确：空值、空列表、重复提交、并发、越权、上级不存在，是否都有确定行为与中文提示。
- 安全边界是否绕过：鉴权注解、租户与数据权限、内部接口隔离、令牌注销。
- 字段是否三处同步：后端契约、前端类型、菜单权限串。
- 本地产物是否入库：`target`、`dist`、`node_modules`、`logs`、`.idea`、`.workbuddy`。

### 依赖与技术债

- 升级依赖、替换库、改构建配置，一律单独提交，并在提交信息里说明原因与影响面。
- 修复用的临时兜底不允许留在主干：要么当次补成正式实现，要么明确记录待办并说明触发条件。
- 重构与功能改动分开；重构只在不改变外部行为的前提下进行，改完必须重跑第八节的三道门禁。

## 九、Git 提交规范

采用 **Conventional Commits**：

```
type(scope): description
```

- `description` 用**中文**，说清改了什么、为什么。
- `type`：`feat` / `fix` / `refactor` / `test` / `docs` / `chore` / `perf` / `style` / `build` / `ci`。
- `scope` 用模块或页面名：`dept`、`menu`、`auth`、`web`、`docs`、`build`。
- 单次提交聚焦一件事；功能与其验证脚本可以同一次提交，格式化与依赖升级必须单独提交。

```
feat(dept): 支持把部门调整到根节点并重算层级路径
fix(menu): 修复根菜单保存时的父级校验误报
test(notify): 补充并发错误提示去重的冒烟用例
```

## 十、完成定义（DoD）

一个改动只有满足以下全部条件才算完成：

1. 后端 `mvn -o -pl system/system-biz -am compile` 通过（`JAVA_HOME` 为 JDK 21；涉及公共模块时带上对应 `-pl`），
   无新增编译告警；
2. 前端 `npm run typecheck` 与 `npm run build` 通过；
3. 契约同步：后端 DTO/VO 字段、`@PreAuthorize` 标识、前端 `types.ts` / `api/*.ts`、菜单 `perms` 一处不落；
4. 行为改动给出可复现的验证：优先补 `web/scripts` 的冒烟脚本或记录实际接口/页面验证过程；
   纯函数与工具类补单元测试（`common-core` 已有 `HttpUtilTest` / `NumUtilTest` 先例）；
5. 注释符合第三节，命名与位置符合第五、六、七节；
6. 按第九节提交，工作区干净。

## 十一、质量红线（评审必查）

- 接口漏标 `@PreAuthorize`，或前后端权限串不一致。
- 把 `0`、`null` 当作真实主键去查库；树形结构绕过 `TreeUtil` 自己写判定。
- Controller 里写业务逻辑、拼 `Wrappers`、try-catch 后自造错误响应。
- 手动 set 公共字段、手动拼 `tenant_id` / `is_deleted` 条件、写侧 DTO 暴露 `tenantId`，或直接 `SELECT *`。
- VO 暴露 `password` 等敏感字段，靠 Service 置 `null` 兜底。
- 树形编辑不排除自身与自身后代、移动节点不重算后代路径、根节点 `parentId` 不归一为 `0`；
  删树节点不拦下级、分组改码不同步子项编码。
- 业务失败用 `null` / `false` 表达，或静默吞异常、用默认值兜底。
- 在循环里逐条查库，或把过滤条件搬到内存里做。
- 前端裸 `axios`、手拼 URL、`any`、`Number(id)`、`message.error` 绕过统一提示。
- 未经验证就宣称完成；构建/类型检查不通过仍提交。
- 提交里混入 `node_modules`、`target`、`dist`、`logs`、`.idea`、`.workbuddy` 等本地产物。

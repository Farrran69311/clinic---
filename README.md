# 医疗诊所管理系统

Java Swing 图形化桌面应用，覆盖医疗诊所日常管理场景，提供医生端与患者端门户、在线专家会诊、病例库、药房库存与 AI 辅助洞察等功能。

## 核心特性

- 持续双写：CSV 写入同时实时镜像到 MySQL，可结合 Navicat 等工具进行在线分析。
- 表格交互增强：所有列表支持搜索过滤、悬停快速预览与双击详情弹窗。
- 全局刷新框架：Swing `Refreshable` 接口配合定时器实现自动刷新，并提供手动刷新按钮；会议纪要、洞察等模块均可实时更新。

## MySQL 双写与数据同步

- 默认启用 CSV→MySQL 持续双写，当存在 MySQL 驱动并能连接到 `clinic` 库时，每次写入 CSV 会同时更新同名 MySQL 表。
- 连接配置支持环境变量或 JVM 参数覆盖：`CLINIC_DB_HOST`、`CLINIC_DB_PORT`、`CLINIC_DB_NAME`、`CLINIC_DB_USER`、`CLINIC_DB_PASSWORD`（默认 `localhost:3306 / clinic / root / 123456`）。
- 请将 `mysql-connector-j` 依赖加入运行时类路径，例如：

  ```bash
  java -cp "out:mysql-connector-j-8.4.0.jar" clinic.ClinicApp
  ```

- 首次迁移：

  ```bash
  mysql -h localhost -P 3306 -u root -p123456 < scripts/mysql/create_schema.sql
  ./scripts/mysql/import_csv.sh
  ```

- 若需临时关闭同步，可设置 `CLINIC_DB_SYNC_ENABLED=false` 或 JVM 参数 `-Dclinic.db.sync.enabled=false`。
- 丰富示例数据：`data/` 目录预置医生、患者、会诊、药品等多科室数据，可直接体验系统流程。

## 环境要求

- JDK 17（或兼容版本）
- 无需额外第三方依赖，使用标准 Java 类库读写 CSV

## 编译与运行

```bash
javac -encoding UTF-8 -d out $(find src -name '*.java')
java -cp out clinic.ClinicApp
```

> Windows 用户可将 `find` 替换为 PowerShell 版本：`Get-ChildItem -Recurse -Filter *.java | ForEach-Object { $_.FullName }`。

常用示例账号：

- 医生端：`doctor` / `doctor123`
- 管理员：`admin` / `admin123`
- 患者端：
  - `alice` / `patient123`
  - `huangxiaoyu` / `patient123`
  - 亦可使用注册入口新建患者账号

## 数据文件概览

所有业务数据保存在 `data/*.csv` 中，字段以 `|` 分隔，修改后立即持久化，可直接编辑以扩展样例。

- `patients.csv`：患者档案含紧急联系人与病史备注
- `doctors.csv`：医生科室、职称、专长与评分
- `appointments.csv`：预约状态、随访备注
- `consultations.csv` / `prescriptions.csv`：问诊总结与处方指引
- `medicines.csv`：药品规格、库存与有效期
- `payments.csv`：患者支付记录、金额、方式、关联对象
- `insurance_claims.csv`：医保理赔申请、审批与打款信息
- `stock_movements.csv`：药品入库/出库/盘点明细与成本
- `audit_logs.csv`：用户操作审计记录
- `expert_sessions.csv` / `expert_participants.csv` / `meeting_minutes.csv`：专家会诊全链路信息
- `case_library.csv`：疑难病例资料
- 其他文件详见设计文档

## 已知问题

- Insight 助理的医生端周总结在当前示例数据下可能为空，可按需补充 `work_progress.csv` 等数据。
- 财务中心当前以 CSV 作为轻量存储，若并发写入需求较高需升级至数据库方案。

## 目录结构

- `src/clinic`：Java 源码（模型、仓库、服务、Swing 界面）
- `data/`：示例 CSV 数据
- `docs/`：系统设计与功能说明
- `TODO.md`：后续工作记录

更多架构、数据设计与交互流程详见 `docs/系统设计说明.md`。

## 发布 Release 包

使用脚本快速生成可分发的压缩包（包含可执行 Jar、数据与说明文档）：

```bash
./scripts/package_release.sh v1.1.0
```

打包成功后，可在 `release/clinic-app-v1.1.0.zip` 获取整套交付物；`RUN.sh`、`RUN.bat` 分别适用于 macOS/Linux 与 Windows 环境。*** End Patch

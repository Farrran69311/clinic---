# 医疗诊所管理系统

Java Swing 图形化桌面应用，覆盖医疗诊所日常管理场景，提供医生端与患者端门户、在线专家会诊、病例库、药房库存与 AI 辅助洞察等功能。

## 核心特性

- 双角色登录：医生、管理员、患者分角色进入对应工作台。
- 预约与排班：支持创建、确认、取消预约并同步日历事件。
- 问诊与处方：记录问诊摘要、生成处方、跟踪发药状态。
- 专家会诊全流程：会诊排期、参与人员、会议纪要、专家建议一体化呈现。
- 疑难病例库与工作进度：沉淀案例经验，追踪患者待办与责任医生。
- Insight 助理：整合患者近期数据生成诊疗要点与周报。
- 全局刷新框架：Swing `Refreshable` 接口配合定时器实现自动刷新，并提供手动刷新按钮；会议纪要、洞察等模块均可实时更新。
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
- `expert_sessions.csv` / `expert_participants.csv` / `meeting_minutes.csv`：专家会诊全链路信息
- `case_library.csv`：疑难病例资料
- 其他文件详见设计文档

## 目录结构

- `src/clinic`：Java 源码（模型、仓库、服务、Swing 界面）
- `data/`：示例 CSV 数据
- `docs/`：系统设计与功能说明
- `TODO.md`：后续工作记录

更多架构、数据设计与交互流程详见 `docs/系统设计说明.md`。

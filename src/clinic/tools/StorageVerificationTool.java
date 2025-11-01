package clinic.tools;

import clinic.AppContext;
import clinic.model.Appointment;
import clinic.model.Medicine;
import clinic.model.Patient;
import clinic.model.Payment;
import clinic.model.StockMovement;
import clinic.model.InsuranceClaim;
import clinic.service.AppointmentService;
import clinic.service.AuditService;
import clinic.service.InsuranceClaimService;
import clinic.service.InventoryService;
import clinic.service.PatientService;
import clinic.service.PaymentService;
import clinic.service.PharmacyService;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class StorageVerificationTool {
    public static void main(String[] args) throws Exception {
        Path projectRoot = Paths.get("").toAbsolutePath();
        Path sourceData = resolveDataDirectory(projectRoot);

        Path tempRoot = Files.createTempDirectory("clinic-storage-test");
        Path workingData = tempRoot.resolve("data");
        copyDirectory(sourceData, workingData);

        AppContext context = new AppContext(workingData);
        PatientService patientService = context.getPatientService();
        PharmacyService pharmacyService = context.getPharmacyService();
        AppointmentService appointmentService = context.getAppointmentService();
        PaymentService paymentService = context.getPaymentService();
        InsuranceClaimService insuranceClaimService = context.getInsuranceClaimService();
        InventoryService inventoryService = context.getInventoryService();
        AuditService auditService = context.getAuditService();

        Patient patient = patientService.createPatient(
            "验收患者",
            "FEMALE",
            LocalDate.now().minusYears(30),
            "13900001111",
            "测试地址",
            "家属 13900002222",
            "自动化存储验证"
        );

        Medicine medicine = pharmacyService.addMedicine(
            "存储验证药品",
            "500mg",
            0,
            "盒",
            LocalDate.now().plusYears(2)
        );

        Appointment appointment = appointmentService.createAppointment(
            patient.getId(),
            context.getDoctorService().listDoctors().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("医生列表为空，无法验证"))
                .getId(),
            LocalDateTime.now().plusDays(1),
            "自动测试预约"
        );

        Payment payment = paymentService.createPayment(
            patient.getId(),
            Payment.RelatedType.APPOINTMENT,
            appointment.getId(),
            new BigDecimal("188.00"),
            "CNY",
            "CREDIT_CARD"
        );
        paymentService.markPaid(payment.getId());

        InsuranceClaim claim = insuranceClaimService.submitClaim(
            payment.getId(),
            "PUBLIC",
            new BigDecimal("0.8"),
            new BigDecimal("150.40"),
            "自动测试理赔"
        );
        insuranceClaimService.approve(claim.getId(), new BigDecimal("150.40"), "审核通过");

        inventoryService.recordInbound(
            medicine.getId(),
            25,
            new BigDecimal("12.50"),
            "VERIFICATION",
            appointment.getId(),
            "system",
            "自动测试入库"
        );

        auditService.logAction(
            "system",
            "ADMIN",
            "VERIFY_STORAGE",
            "APPOINTMENT",
            appointment.getId(),
            "验证流程写入",
            "SUCCESS",
            "127.0.0.1"
        );

        AppContext reloadedContext = new AppContext(workingData);

        boolean patientPersisted = reloadedContext.getPatientService().listPatients().stream()
            .anyMatch(p -> Objects.equals(p.getId(), patient.getId()));
        boolean appointmentPersisted = reloadedContext.getAppointmentService().listAppointments().stream()
            .anyMatch(a -> Objects.equals(a.getId(), appointment.getId()));
        boolean paymentPersisted = reloadedContext.getPaymentService().listAll().stream()
            .anyMatch(p -> Objects.equals(p.getId(), payment.getId()) && p.getStatus() == Payment.Status.PAID);
        boolean claimPersisted = reloadedContext.getInsuranceClaimService().listAll().stream()
            .anyMatch(c -> Objects.equals(c.getId(), claim.getId()) && c.getStatus() == InsuranceClaim.Status.APPROVED);
        boolean stockPersisted = !reloadedContext.getInventoryService().listByMedicine(medicine.getId()).isEmpty();
        boolean auditPersisted = !reloadedContext.getAuditService().listAll().isEmpty()
            && reloadedContext.getAuditService().listAll().stream()
                .anyMatch(log -> Objects.equals(log.getEntityId(), appointment.getId())
                    && Objects.equals(log.getAction(), "VERIFY_STORAGE"));

        int onHand = reloadedContext.getInventoryService().calculateOnHandQuantity(medicine.getId());
        List<StockMovement> movements = reloadedContext.getInventoryService().listByMedicine(medicine.getId());
        movements.sort(Comparator.comparing(StockMovement::getOccurredAt));

        if (patientPersisted && appointmentPersisted && paymentPersisted
            && claimPersisted && stockPersisted && auditPersisted && onHand >= 25) {
            System.out.println("✅ 数据持久化验证通过");
        } else {
            throw new IllegalStateException("数据持久化验证失败: "
                + "patient=" + patientPersisted
                + ", appointment=" + appointmentPersisted
                + ", payment=" + paymentPersisted
                + ", claim=" + claimPersisted
                + ", stock=" + stockPersisted
                + ", audit=" + auditPersisted
                + ", onHand=" + onHand);
        }

        System.out.println("工作数据目录: " + workingData);
        System.out.printf("库存记录 %d 条，最新入库成本 %.2f\n",
            movements.size(),
            movements.isEmpty() ? 0.0 : movements.get(movements.size() - 1).getUnitCost());
    }

    private static void copyDirectory(Path source, Path target) throws IOException {
        Files.createDirectories(target);
        try (var paths = Files.walk(source)) {
            paths.forEach(path -> {
                Path destination = target.resolve(source.relativize(path));
                try {
                    if (Files.isDirectory(path)) {
                        Files.createDirectories(destination);
                    } else {
                        Files.copy(path, destination);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("复制文件失败: " + path, e);
                }
            });
        }
    }

    private static Path resolveDataDirectory(Path start) {
        Path candidate = start.resolve("data");
        if (Files.isDirectory(candidate)) {
            return candidate;
        }
        Path parent = start.getParent();
        if (parent != null) {
            candidate = parent.resolve("data");
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("未找到 data 目录，请在项目根目录或 src 目录下运行。当前路径: " + start);
    }
}

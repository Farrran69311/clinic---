package clinic.service;

import clinic.model.CaseRecord;
import clinic.persistence.CaseRecordRepository;
import clinic.persistence.CsvDataStore;

import java.io.IOException;
import java.util.List;

public class CaseRecordService {
    private final CaseRecordRepository caseRecordRepository;

    public CaseRecordService(CaseRecordRepository caseRecordRepository) {
        this.caseRecordRepository = caseRecordRepository;
    }

    public List<CaseRecord> listAll() throws IOException {
        return caseRecordRepository.findAll();
    }

    public List<CaseRecord> listByPatient(String patientId) throws IOException {
        return caseRecordRepository.findByPatient(patientId);
    }

    public CaseRecord createCaseRecord(String patientId, String title, String summary, String tags, String attachment) throws IOException {
        CaseRecord record = new CaseRecord(
            CsvDataStore.randomId(),
            patientId,
            title,
            summary,
            tags,
            attachment
        );
        caseRecordRepository.save(record);
        return record;
    }

    public void updateCaseRecord(CaseRecord record) throws IOException {
        caseRecordRepository.save(record);
    }

    public void deleteCaseRecord(String id) throws IOException {
        caseRecordRepository.deleteById(id);
    }
}

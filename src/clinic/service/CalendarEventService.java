package clinic.service;

import clinic.model.CalendarEvent;
import clinic.persistence.CalendarEventRepository;
import clinic.persistence.CsvDataStore;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

public class CalendarEventService {
    private final CalendarEventRepository calendarEventRepository;

    public CalendarEventService(CalendarEventRepository calendarEventRepository) {
        this.calendarEventRepository = calendarEventRepository;
    }

    public List<CalendarEvent> listAll() throws IOException {
        return calendarEventRepository.findAll();
    }

    public List<CalendarEvent> listByOwner(String doctorId) throws IOException {
        return calendarEventRepository.findByOwner(doctorId);
    }

    public CalendarEvent createEvent(String title, LocalDateTime start, LocalDateTime end, String patientId, String ownerDoctorId, String location, String notes) throws IOException {
        CalendarEvent event = new CalendarEvent(
            CsvDataStore.randomId(),
            title,
            start,
            end,
            patientId,
            ownerDoctorId,
            location,
            notes
        );
        calendarEventRepository.save(event);
        return event;
    }

    public void updateEvent(CalendarEvent event) throws IOException {
        calendarEventRepository.save(event);
    }

    public void deleteEvent(String id) throws IOException {
        calendarEventRepository.deleteById(id);
    }
}

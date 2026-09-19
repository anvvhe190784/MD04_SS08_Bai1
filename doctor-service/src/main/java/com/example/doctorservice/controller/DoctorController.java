package com.example.doctorservice.controller;

import com.example.doctorservice.model.DoctorScheduleDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/doctors")
public class DoctorController {

    private static final Logger log = LoggerFactory.getLogger(DoctorController.class);

    private final Map<Long, DoctorScheduleDto> mockDatabase = new ConcurrentHashMap<>();

    public DoctorController() {
        mockDatabase.put(1L, new DoctorScheduleDto(1L, "Dr. Nguyen Van A", "Tim mach", "08:00 - 11:30", true));
        mockDatabase.put(2L, new DoctorScheduleDto(2L, "Dr. Tran Thi B", "Nhi khoa", "13:30 - 17:00", true));
        mockDatabase.put(3L, new DoctorScheduleDto(3L, "Dr. Le Van C", "Rang Ham Mat", "09:00 - 12:00", false));
    }

    @GetMapping("/{doctorId}/schedule")
    public ResponseEntity<DoctorScheduleDto> getSchedule(@PathVariable Long doctorId) {
        log.info("[Doctor-Service:8082] Nhan yeu cau kiem tra lich truc cho bac si ID: {}", doctorId);
        DoctorScheduleDto dto = mockDatabase.get(doctorId);
        if (dto == null) {
            dto = new DoctorScheduleDto(doctorId, "Dr. Default Specialist", "Da khoa", "08:00 - 17:00", true);
        }
        return ResponseEntity.ok(dto);
    }
}

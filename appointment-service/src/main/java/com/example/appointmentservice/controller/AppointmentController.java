package com.example.appointmentservice.controller;

import com.example.appointmentservice.dto.AppointmentRequest;
import com.example.appointmentservice.dto.AppointmentResponse;
import com.example.appointmentservice.dto.DoctorScheduleDto;
import com.example.appointmentservice.service.AppointmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    /**
     * API kiem tra lich truc bac si - co tich hop Circuit Breaker doctorServiceCB
     */
    @GetMapping("/check-doctor/{doctorId}")
    public ResponseEntity<AppointmentResponse> checkDoctorSchedule(@PathVariable Long doctorId) {
        DoctorScheduleDto schedule = appointmentService.getDoctorSchedule(doctorId);
        String cbState = appointmentService.getCircuitBreakerState();

        boolean isSuccess = schedule != null && schedule.isAvailable() && !schedule.getDoctorName().startsWith("N/A");
        String message = isSuccess
                ? "Lay thong tin lich truc bac si thanh cong."
                : "Khong the lay lich truc bac si (Doctor-Service sâp hoac Circuit Breaker dang OPEN).";

        return ResponseEntity.ok(new AppointmentResponse(
                isSuccess,
                message,
                null,
                schedule,
                cbState
        ));
    }

    /**
     * API dat lich hen kham benh
     */
    @PostMapping("/book")
    public ResponseEntity<AppointmentResponse> bookAppointment(@RequestBody AppointmentRequest request) {
        AppointmentResponse response = appointmentService.bookAppointment(request);
        return ResponseEntity.ok(response);
    }

    /**
     * API kiem tra trang thai hien tai va chi so metrics cua Circuit Breaker doctorServiceCB
     */
    @GetMapping("/circuit-breaker-status")
    public ResponseEntity<Map<String, Object>> getCircuitBreakerStatus() {
        return ResponseEntity.ok(appointmentService.getCircuitBreakerDetails());
    }
}

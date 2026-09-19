package com.example.appointmentservice.service;

import com.example.appointmentservice.dto.AppointmentRequest;
import com.example.appointmentservice.dto.AppointmentResponse;
import com.example.appointmentservice.dto.DoctorScheduleDto;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);
    private static final String DOCTOR_SERVICE_CB = "doctorServiceCB";

    private final RestTemplate restTemplate;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final String doctorServiceUrl;

    public AppointmentService(
            RestTemplate restTemplate,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${doctor.service.url:http://localhost:8082}") String doctorServiceUrl) {
        this.restTemplate = restTemplate;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.doctorServiceUrl = doctorServiceUrl;
    }

    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = DOCTOR_SERVICE_CB, fallbackMethod = "doctorScheduleFallback")
    public DoctorScheduleDto getDoctorSchedule(Long doctorId) {
        String url = doctorServiceUrl + "/doctors/" + doctorId + "/schedule";
        log.info("[Appointment-Service] Goi sang Doctor-Service tai URL: {}", url);
        return restTemplate.getForObject(url, DoctorScheduleDto.class);
    }

    /**
     * Ham Fallback duoc goi khi:
     * 1. Doctor-Service bi sap hoac throw Exception
     * 2. Circuit Breaker dang o trang thai OPEN (ngat mach)
     */
    public DoctorScheduleDto doctorScheduleFallback(Long doctorId, Throwable throwable) {
        String cbState = getCircuitBreakerState();
        log.warn("[FALLBACK ACTIVATED] Doctor-Service gap su co hoac mach OPEN! DoctorId: {}, Trang thai mach: {}, Nguyen nhan: {}",
                doctorId, cbState, throwable.getClass().getSimpleName() + " - " + throwable.getMessage());

        DoctorScheduleDto fallbackDto = new DoctorScheduleDto();
        fallbackDto.setDoctorId(doctorId);
        fallbackDto.setDoctorName("N/A (Dich vu bac si tam thoi khong kha dung)");
        fallbackDto.setSpecialty("N/A");
        fallbackDto.setAvailableTime("N/A");
        fallbackDto.setAvailable(false);
        return fallbackDto;
    }

    public AppointmentResponse bookAppointment(AppointmentRequest request) {
        Long doctorId = request.getDoctorId();
        DoctorScheduleDto schedule = getDoctorSchedule(doctorId);
        String cbState = getCircuitBreakerState();

        if (schedule == null || !schedule.isAvailable() || schedule.getDoctorName().startsWith("N/A")) {
            return new AppointmentResponse(
                    false,
                    "Khong the dat lich hen. Doctor-Service dang gap su co hoac ngat mach (Trang thai Circuit Breaker: " + cbState + ").",
                    null,
                    schedule,
                    cbState
            );
        }

        long appointmentId = ThreadLocalRandom.current().nextLong(1000, 9999);
        return new AppointmentResponse(
                true,
                "Dat lich hen thanh cong cho benh nhan " + request.getPatientName() + " voi bac si " + schedule.getDoctorName(),
                appointmentId,
                schedule,
                cbState
        );
    }

    public String getCircuitBreakerState() {
        try {
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(DOCTOR_SERVICE_CB);
            return cb.getState().name();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    public Map<String, Object> getCircuitBreakerDetails() {
        Map<String, Object> details = new HashMap<>();
        try {
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(DOCTOR_SERVICE_CB);
            CircuitBreaker.Metrics metrics = cb.getMetrics();
            details.put("name", cb.getName());
            details.put("state", cb.getState().name());
            details.put("failureRate", metrics.getFailureRate() + "%");
            details.put("bufferedCalls", metrics.getNumberOfBufferedCalls());
            details.put("failedCalls", metrics.getNumberOfFailedCalls());
            details.put("successfulCalls", metrics.getNumberOfSuccessfulCalls());
            details.put("slowCalls", metrics.getNumberOfSlowCalls());
        } catch (Exception e) {
            details.put("error", e.getMessage());
        }
        return details;
    }
}

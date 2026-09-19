package com.example.appointmentservice;

import com.example.appointmentservice.dto.DoctorScheduleDto;
import com.example.appointmentservice.service.AppointmentService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
public class AppointmentCircuitBreakerTest {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @MockBean
    private RestTemplate restTemplate;

    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        circuitBreaker = circuitBreakerRegistry.circuitBreaker("doctorServiceCB");
        circuitBreaker.reset();
    }

    @Test
    @DisplayName("Kiem tra trang thai ban dau cua Circuit Breaker phai la CLOSED")
    void testInitialStateIsClosed() {
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState(),
                "Trang thai ban dau cua Circuit Breaker phai la CLOSED");
    }

    @Test
    @DisplayName("Khi Doctor-Service hoat dong binh thuong, goi API thanh cong va mach giu CLOSED")
    void testSuccessfulCallsKeepCircuitClosed() {
        DoctorScheduleDto mockDto = new DoctorScheduleDto(1L, "Dr. Nguyen Van A", "Tim mach", "08:00 - 11:30", true);
        when(restTemplate.getForObject(anyString(), eq(DoctorScheduleDto.class))).thenReturn(mockDto);

        DoctorScheduleDto result = appointmentService.getDoctorSchedule(1L);

        assertNotNull(result);
        assertEquals("Dr. Nguyen Van A", result.getDoctorName());
        assertTrue(result.isAvailable());
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
        verify(restTemplate, times(1)).getForObject(anyString(), eq(DoctorScheduleDto.class));
    }

    @Test
    @DisplayName("Khi Doctor-Service bi sap: Sau 5 lan goi that bai, Circuit Breaker chuyen sang trang thai OPEN")
    void testCircuitBreakerOpensAfterFiveFailures() {
        // Gia lap Doctor-Service bi sap (Connection refused / ResourceAccessException)
        when(restTemplate.getForObject(anyString(), eq(DoctorScheduleDto.class)))
                .thenThrow(new ResourceAccessException("Connection refused: connect"));

        // Thuc hien 5 lan goi (theo cau hinh: minimum-number-of-calls=5, failure-rate-threshold=50%)
        for (int i = 1; i <= 5; i++) {
            DoctorScheduleDto fallbackResult = appointmentService.getDoctorSchedule(1L);
            assertNotNull(fallbackResult, "Ket qua fallback khong duoc null tai lan goi " + i);
            assertFalse(fallbackResult.isAvailable(), "Ket qua fallback phai co available=false");
            assertTrue(fallbackResult.getDoctorName().contains("N/A"), "Ket qua fallback phai co ten chua N/A");
        }

        // Kiem tra trang thai Circuit Breaker da chuyen sang OPEN chua
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState(),
                "Circuit Breaker phai chuyen sang trang thai OPEN sau 5 lan goi that bai lien tiep");

        // Khi mach da OPEN: Lan goi thu 6 phai duoc ngat mach ngay lap tuc (short-circuit) ma KHONG goi tiep qua RestTemplate
        DoctorScheduleDto callWhenOpen = appointmentService.getDoctorSchedule(1L);
        assertNotNull(callWhenOpen);
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());

        // RestTemplate chi duoc goi dung 5 lan ban dau, lan thu 6 bi ngat ngay lap tuc boi Circuit Breaker
        verify(restTemplate, times(5)).getForObject(anyString(), eq(DoctorScheduleDto.class));
    }

    @Test
    @DisplayName("Kiem tra co che HALF_OPEN sau khi mach mo")
    void testHalfOpenTransition() {
        // Chuyen mach sang OPEN
        circuitBreaker.transitionToOpenState();
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());

        // Chuyen sang HALF_OPEN
        circuitBreaker.transitionToHalfOpenState();
        assertEquals(CircuitBreaker.State.HALF_OPEN, circuitBreaker.getState());
    }
}

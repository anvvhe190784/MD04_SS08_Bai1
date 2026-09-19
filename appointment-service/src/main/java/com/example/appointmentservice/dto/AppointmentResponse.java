package com.example.appointmentservice.dto;

public class AppointmentResponse {
    private boolean success;
    private String message;
    private Long appointmentId;
    private DoctorScheduleDto doctorSchedule;
    private String circuitBreakerState;

    public AppointmentResponse() {
    }

    public AppointmentResponse(boolean success, String message, Long appointmentId, DoctorScheduleDto doctorSchedule, String circuitBreakerState) {
        this.success = success;
        this.message = message;
        this.appointmentId = appointmentId;
        this.doctorSchedule = doctorSchedule;
        this.circuitBreakerState = circuitBreakerState;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Long appointmentId) {
        this.appointmentId = appointmentId;
    }

    public DoctorScheduleDto getDoctorSchedule() {
        return doctorSchedule;
    }

    public void setDoctorSchedule(DoctorScheduleDto doctorSchedule) {
        this.doctorSchedule = doctorSchedule;
    }

    public String getCircuitBreakerState() {
        return circuitBreakerState;
    }

    public void setCircuitBreakerState(String circuitBreakerState) {
        this.circuitBreakerState = circuitBreakerState;
    }
}

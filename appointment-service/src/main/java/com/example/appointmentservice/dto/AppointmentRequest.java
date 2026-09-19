package com.example.appointmentservice.dto;

public class AppointmentRequest {
    private Long doctorId;
    private String patientName;
    private String appointmentDate;
    private String notes;

    public AppointmentRequest() {
    }

    public AppointmentRequest(Long doctorId, String patientName, String appointmentDate, String notes) {
        this.doctorId = doctorId;
        this.patientName = patientName;
        this.appointmentDate = appointmentDate;
        this.notes = notes;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(String appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

package com.example.doctorservice.model;

public class DoctorScheduleDto {
    private Long doctorId;
    private String doctorName;
    private String specialty;
    private String availableTime;
    private boolean available;

    public DoctorScheduleDto() {
    }

    public DoctorScheduleDto(Long doctorId, String doctorName, String specialty, String availableTime, boolean available) {
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.specialty = specialty;
        this.availableTime = availableTime;
        this.available = available;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public String getAvailableTime() {
        return availableTime;
    }

    public void setAvailableTime(String availableTime) {
        this.availableTime = availableTime;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public String toString() {
        return "DoctorScheduleDto{" +
                "doctorId=" + doctorId +
                ", doctorName='" + doctorName + '\'' +
                ", specialty='" + specialty + '\'' +
                ", availableTime='" + availableTime + '\'' +
                ", available=" + available +
                '}';
    }
}

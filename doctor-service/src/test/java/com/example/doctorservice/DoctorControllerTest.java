package com.example.doctorservice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class DoctorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Kiem tra API GET /doctors/{id}/schedule tra ve thong tin lich truc bac si")
    void testGetDoctorSchedule() throws Exception {
        mockMvc.perform(get("/doctors/1/schedule"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.doctorId").value(1))
                .andExpect(jsonPath("$.doctorName").value("Dr. Nguyen Van A"))
                .andExpect(jsonPath("$.specialty").value("Tim mach"))
                .andExpect(jsonPath("$.available").value(true));
    }
}

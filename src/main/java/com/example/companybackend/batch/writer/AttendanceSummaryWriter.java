package com.example.companybackend.batch.writer;

import com.example.companybackend.entity.AttendanceSummary;
import com.example.companybackend.repository.AttendanceSummaryRepository;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

public class AttendanceSummaryWriter implements ItemWriter<AttendanceSummary> {

    @Autowired
    private AttendanceSummaryRepository attendanceSummaryRepository;

    @Override
    public void write(Chunk<? extends AttendanceSummary> chunk) throws Exception {
        attendanceSummaryRepository.saveAll(chunk.getItems());
    }
}

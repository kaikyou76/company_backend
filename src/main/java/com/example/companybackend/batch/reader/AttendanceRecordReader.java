package com.example.companybackend.batch.reader;

import com.example.companybackend.entity.AttendanceRecord;
import com.example.companybackend.repository.AttendanceRecordRepository;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class AttendanceRecordReader {

    private final AttendanceRecordRepository attendanceRecordRepository;

    @Autowired
    public AttendanceRecordReader(AttendanceRecordRepository attendanceRecordRepository) {
        this.attendanceRecordRepository = attendanceRecordRepository;
    }

    public RepositoryItemReader<AttendanceRecord> reader() {
        // 使用正确的类型 Map<String, Sort.Direction>
        Map<String, Sort.Direction> sorts = new HashMap<>();
        sorts.put("id", Sort.Direction.ASC);
        
        // 使用RepositoryItemReaderBuilder构建reader
        return new RepositoryItemReaderBuilder<AttendanceRecord>()
                .repository(attendanceRecordRepository)
                .methodName("findAll")
                .pageSize(100)
                .sorts(sorts)
                .name("attendanceRecordReader")
                .build();
    }
}
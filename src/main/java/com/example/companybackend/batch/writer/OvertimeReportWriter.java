package com.example.companybackend.batch.writer;

import com.example.companybackend.entity.OvertimeReport;
import com.example.companybackend.repository.OvertimeReportRepository;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 残業レポートライター
 * 残業監視バッチで生成された残業レポートをデータベースに保存
 */
public class OvertimeReportWriter implements ItemWriter<OvertimeReport> {

    @Autowired
    private OvertimeReportRepository overtimeReportRepository;

    @Override
    public void write(Chunk<? extends OvertimeReport> chunk) throws Exception {
        overtimeReportRepository.saveAll(chunk.getItems());
    }
}
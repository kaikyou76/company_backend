package com.example.companybackend.service;

import com.example.companybackend.entity.Department;
import com.example.companybackend.repository.DepartmentRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    public Optional<Department> getDepartmentById(Integer id) {
        return departmentRepository.findById(id);
    }

    public Department createDepartment(Department department) {
        department.setCreatedAt(OffsetDateTime.now());
        department.setUpdatedAt(OffsetDateTime.now());
        return departmentRepository.save(department);
    }

    public Department updateDepartment(Integer id, Department departmentDetails) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found with id: " + id));

        department.setName(departmentDetails.getName());
        department.setCode(departmentDetails.getCode());
        department.setManagerId(departmentDetails.getManagerId());
        // created_atは変更しない
        department.setUpdatedAt(OffsetDateTime.now());

        return departmentRepository.save(department);
    }

    public void deleteDepartment(Integer id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found with id: " + id));
        
        departmentRepository.delete(department);
    }

    public boolean existsByName(String name) {
        return departmentRepository.existsByName(name);
    }

    public boolean existsByCode(String code) {
        // 修复：使用正确的existsByCode方法
        return departmentRepository.existsByCode(code);
    }
}
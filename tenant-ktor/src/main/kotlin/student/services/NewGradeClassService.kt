package com.example.student.services

import com.example.student.dtos.requests.CreateNewGradeClassRequest
import com.example.student.models.NewGradeClassModel
import com.example.student.repos.NewGradeClassRepository

//object NewGradeClassService {
//
//    fun createGradeClass(request: CreateNewGradeClassRequest): NewGradeClassModel {
//        return NewGradeClassRepository.create(
//            name = request.name.trim(),
//            isActive = true // or request.isActive if you include it in DTO
//        )
//    }
//}
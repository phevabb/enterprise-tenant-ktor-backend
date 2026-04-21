package com.example.student

import org.jetbrains.exposed.dao.id.IntIdTable
import com.example.account.AccountTable
import com.example.student.tables.NewGradeClassTable


object StudentsTable : IntIdTable("student_profile") {

    /**
     * Django: OneToOneField(User)
     * SQL: UNIQUE foreign key to accounts(id)
     */
    val user = reference("user", AccountTable)
        .uniqueIndex()

    /**
     * Django: ForeignKey(NewGradeClass, null=True, blank=True)
     */
    val currentNewGradeClass = reference(
        "current_new_grade_class_id",
        NewGradeClassTable
    ).nullable()

    val isGraduated = bool("is_graduated").default(false)
    val lastSchoolAttended = varchar("last_school_attended", 255).nullable()
    val isDiscountedStudent = bool("is_discounted_student").default(false)
    val isImmunized = bool("is_immunized").default(false)
    val hasAllergies = bool("has_allergies").default(false)
    val hasPeculiarHealthIssues = bool("has_peculiar_health_issues").default(false)
    val allergicFoods = varchar("allergic_foods", 300).nullable()
    val healthIssues = varchar("health_issues", 150).nullable()
    val otherRelatedInfo = text("other_related_info").nullable()
    val nameOfFather = varchar("name_of_father", 100).nullable()
    val nameOfMother = varchar("name_of_mother", 100).nullable()
    val occupationOfFather = varchar("occupation_of_father", 100).nullable()
    val occupationOfMother = varchar("occupation_of_mother", 100).nullable()
    val nationalityOfFather = varchar("nationality_of_father", 100).nullable()
    val nationalityOfMother = varchar("nationality_of_mother", 100).nullable()
    val contactOfFather = varchar("contact_of_father", 100).nullable()
    val contactOfMother = varchar("contact_of_mother", 100).nullable()
    val houseNumber = varchar("house_number", 100).nullable()
}
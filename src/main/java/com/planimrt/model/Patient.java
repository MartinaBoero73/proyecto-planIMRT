package com.planimrt.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "patients")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String patientId;

    @Column(nullable = false, length = 200)
    private String name;

    private LocalDate birthDate;

    @Column(length = 10)
    private String sex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_user_id", nullable = false)
    private User responsibleUser;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL)
    private List<DicomFile> dicomFiles;

}
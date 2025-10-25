package com.planimrt.repository;

import com.planimrt.model.DicomFile;
import com.planimrt.model.McsResult;
import com.planimrt.model.ProcessingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface McsResultRepository extends JpaRepository<McsResult, Long> {

    Optional<McsResult> findByDicomFile(DicomFile dicomFile);

    List<McsResult> findByStatus(ProcessingStatus status);

    @Query("SELECT m FROM McsResult m WHERE m.mcsValue >= :minValue AND m.mcsValue <= :maxValue")
    List<McsResult> findByMcsValueBetween(@Param("minValue") Double minValue,
                                          @Param("maxValue") Double maxValue);

    @Query("SELECT AVG(m.mcsValue) FROM McsResult m WHERE m.status = 'SUCCESS'")
    Double getAverageMcsValue();
}
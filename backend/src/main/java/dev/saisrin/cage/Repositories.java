package dev.saisrin.cage;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.jpa.repository.*;

interface Users extends JpaRepository<CageUser, Long> {
  Optional<CageUser> findByUsername(String name);
}

interface Properties extends JpaRepository<Property, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Property p where p.id=1")
  Property lock();
}

interface Patrons extends JpaRepository<Patron, Long> {}

interface Types extends JpaRepository<TransactionType, Long> {}

interface Transactions extends JpaRepository<CageTransaction, Long> {
  Optional<CageTransaction> findBySourceId(String source);

  List<CageTransaction> findByPatronIdAndGamingDayAndStatus(Long id, LocalDate day, String status);
}

interface Reports extends JpaRepository<Report, Long> {
  Optional<Report> findByKindAndGroupKey(String kind, String groupKey);
}

interface Revisions extends JpaRepository<ReportRevision, Long> {
  List<ReportRevision> findByReportIdOrderByRevisionDesc(Long id);
}

interface Batches extends JpaRepository<FilingBatch, Long> {}

interface Items extends JpaRepository<BatchItem, Long> {
  List<BatchItem> findByBatchId(Long id);

  boolean existsByReportIdAndRevision(Long id, int revision);
}

interface Audits extends JpaRepository<AuditEvent, Long> {}

interface Tasks extends JpaRepository<WorkTask, Long> {}

interface Configurations extends JpaRepository<Configuration, Long> {
  Optional<Configuration> findByCode(String code);
}

interface Attachments extends JpaRepository<Attachment, Long> {
  List<Attachment> findByOwnerTypeAndOwnerId(String type, Long id);
}

interface Subjects extends JpaRepository<ReportSubject, Long> {
  List<ReportSubject> findByReportId(Long id);
}

package dev.saisrin.cage;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@MappedSuperclass
abstract class Row {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Version public long version;
}

@Entity
@Table(name = "cage_user")
class CageUser extends Row {
  @Column(unique = true, nullable = false)
  public String username;

  @JsonIgnore public String password;
  public String displayName;
  public String roles;
  public boolean enabled = true;
}

@Entity
class Property extends Row {
  public String name = "Juniper Casino — Demonstration";
  public String zone = "America/New_York";
  public String cutoff = "06:00";
  public String reportingScope = "DEMO-US-01";
  public String contact = "compliance@example.invalid";
}

@Entity
class Patron extends Row {
  public String legalName;
  public String aliasName;
  public LocalDate dateOfBirth;
  public String address;
  public String country = "US";
  public String contact;
  public String occupation;

  @Column(unique = true)
  public String loyaltyId;

  @Convert(converter = SecretConverter.class)
  @JsonIgnore
  public String tin;

  @Convert(converter = SecretConverter.class)
  @JsonIgnore
  public String identityNumber;

  public String identityType = "DEMO_ID";
  public String issuingAuthority = "Synthetic fixture";
  public LocalDate identityExpiry;
  public String identityStatus = "UNVERIFIED";
  public String residency = "RESIDENT";
  public String w9Status = "REQUESTED";
  @ManyToOne public Patron mergedInto;
  public boolean temporary;
}

@Entity
class TransactionType extends Row {
  @Column(unique = true)
  public String code;

  public String label;
  public String treatment;
  public String requiredField = "reference";
  public String taxGame;
  public boolean active;
  public String state = "PROPOSED";
  public String createdBy;
  public String approvedBy;
  public LocalDate effectiveFrom = LocalDate.of(2026, 1, 1);
  public LocalDate effectiveTo;
}

@Entity
@Table(name = "cage_transaction")
class CageTransaction extends Row {
  @Column(unique = true, nullable = false)
  public String sourceId;

  @ManyToOne(optional = false)
  public Patron patron;

  @ManyToOne public Patron conductor;
  public String relationship;

  @ManyToOne(optional = false)
  public TransactionType type;

  public String typeLabel;
  public String treatment;
  public long typeVersion;
  public BigDecimal gross;
  public BigDecimal cash;
  public BigDecimal cashIn = BigDecimal.ZERO;
  public BigDecimal cashOut = BigDecimal.ZERO;
  public String currency = "USD";
  public BigDecimal exchangeRate = BigDecimal.ONE;
  public Instant occurredAt;
  public LocalDate gamingDay;
  public String propertyZone;
  public String gamingCutoff;
  public long ruleVersion;
  public String station;
  public String cashier;
  public String reference;
  public String notes;
  public String status = "DRAFT";
  @ManyToOne public CageTransaction corrects;
  public Instant createdAt = Instant.now();
}

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"kind", "group_key"}))
class Report extends Row {
  public String kind;

  @Column(name = "group_key")
  public String groupKey;

  @ManyToOne(optional = false)
  public Patron patron;

  public LocalDate gamingDay;
  public BigDecimal cashIn = BigDecimal.ZERO;
  public BigDecimal cashOut = BigDecimal.ZERO;
  public BigDecimal amount = BigDecimal.ZERO;
  public String formType;
  public String game;
  public String taxRuleVersion;
  public BigDecimal withholding = BigDecimal.ZERO;
  public String status = "DRAFT";
  public String externalOutcome = "UNCONFIRMED";
  public String filingStatus = "PREPARED";
  public String creator;
  public String approver;
  public int revision = 1;
  public Integer approvedRevision;
  public LocalDate dueDate;
  public LocalDate detectionDate;
  public LocalDate activityStart;
  public LocalDate activityEnd;

  @Column(columnDefinition = "text")
  @Convert(converter = SecretConverter.class)
  public String narrative;

  public String category;
  public String filingType = "INITIAL";

  @Column(columnDefinition = "text")
  @Convert(converter = SecretConverter.class)
  public String fields = "{}";

  @Column(columnDefinition = "text")
  @Convert(converter = SecretConverter.class)
  public String snapshot;

  public boolean obligationOpen = true;
  public Instant updatedAt = Instant.now();
  public String externalId;
  @ManyToOne public Report corrects;
}

@Entity
class ReportRevision extends Row {
  @ManyToOne(optional = false)
  public Report report;

  public int revision;

  @Column(columnDefinition = "text")
  @Convert(converter = SecretConverter.class)
  public String snapshot;

  public String actor;
  public String reason;
  public Instant createdAt = Instant.now();
}

@Entity
class FilingBatch extends Row {
  public String kind;
  public String creator;
  public String status = "PREPARED";
  public String scenario = "ACCEPT";
  public String checksum;
  public String schemaVersion = "CAGE-MOCK-1";
  public String transportReceipt;
  public Instant createdAt = Instant.now();
  public Instant submittedAt;
  public Instant respondedAt;

  @Column(columnDefinition = "text")
  public String rawResponse;
}

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"report_id", "revision"}))
class BatchItem extends Row {
  @ManyToOne(optional = false)
  public FilingBatch batch;

  @ManyToOne(optional = false)
  public Report report;

  public int revision;

  @Column(columnDefinition = "text")
  @Convert(converter = SecretConverter.class)
  public String snapshot;

  public String outcome = "UNCONFIRMED";
  public String externalId;
  public String message;
}

@Entity
class AuditEvent extends Row {
  public String scope;
  public String entityType;
  public Long entityId;
  public String action;
  public String actor;
  public Instant occurredAt = Instant.now();
  public String correlationId;

  @Column(columnDefinition = "text")
  public String reason;

  @Column(columnDefinition = "text")
  @Convert(converter = SecretConverter.class)
  public String beforeValue;

  @Column(columnDefinition = "text")
  @Convert(converter = SecretConverter.class)
  public String afterValue;
}

@Entity
class WorkTask extends Row {
  public String scope;
  public String title;
  public Long reportId;
  public boolean resolved;
  public String resolution;
  public Instant createdAt = Instant.now();
}

@Entity
class Configuration extends Row {
  @Column(unique = true)
  public String code;

  @Column(columnDefinition = "text")
  public String payload;

  public String reason;
  public String changedBy;
}

@Entity
class Attachment extends Row {
  public String ownerType;
  public Long ownerId;
  public String filename;
  public String mediaType;
  public int bytes;
  public String uploadedBy;
  public Instant createdAt = Instant.now();

  @Column(columnDefinition = "text")
  @Convert(converter = SecretConverter.class)
  @JsonIgnore
  public String content;
}

@Entity
class ReportSubject extends Row {
  @ManyToOne(optional = false)
  public Report report;

  @ManyToOne(optional = false)
  public Patron patron;

  public String relationship;
}

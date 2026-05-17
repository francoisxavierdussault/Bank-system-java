# Fortis Bank System

Desktop banking application built in Java with a Swing GUI and an Oracle XE database. Supports two roles — Manager and Customer — each with their own interface and permissions.

## Stack

- **Java 17**
- **Swing** (AWT EventQueue)
- **Oracle XE 21c** — JDBC thin driver (ojdbc8)
- **Maven** (Eclipse / IntelliJ IDEA)
- **draw.io** — UML diagrams
- **Javadoc**

## Prerequisites

- Java 17+
- Oracle XE 21c running locally on port 1521
- ojdbc8.jar (included in project root)
- Maven 3.6+

## Setup

### 1. Create the database schema

Connect to Oracle as `sysdba` and run:

```sql
@sql/SETUP.sql
```

This creates the `c##banksystem` user, all tables, and seed data.

### 2. Compile

```bash
mvn clean compile
```

### 3. Run

```bash
mvn exec:java -Dexec.mainClass="banksystem.ui.Main"
```

Or run `Main.java` directly from your IDE. The login window opens first and routes to the correct interface based on the authenticated user's role.

## Architecture

```
src/banksystem/
├── business/     Abstract Account + 4 concrete types, Customer,
│                 Transaction, Validator, ITransaction interface
├── dao/          One DAO per entity; shared Oracle singleton (DBConnection)
├── dto/          Plain data transfer objects between layers
├── service/      LoginService, CustomerService, ManagerService
├── ui/           LoginWindow, BankManager (manager), BankCustomer (client)
├── util/         BankLogger, CsvExporter, PdfExporter, PasswordUtil,
│                 SessionManager
├── enums/        EnumAccountType, EnumAccountStatus, EnumTransactionType
└── exceptions/   InsufficientFundException, NotInRangeException,
                  NotNullException
```

## Account Types

| Type | Description |
|---|---|
| `CheckingAccount` | Daily withdrawal limit ($5 000 default) |
| `SavingsAccount` | Interest-bearing; restricted withdrawal frequency |
| `CreditAccount` | Credit line with configurable limit and interest rate |
| `CurrencyAccount` | Stores a base currency and conversion rate |

## Authentication

- Two roles: `MANAGER` and `CUSTOMER`
- Passwords hashed via `PasswordUtil`
- **First login:** numeric user ID accepted as temporary password, hashed immediately on first successful auth
- **3 failed attempts** locks the account in Oracle
- Session stored in `SessionManager` (singleton) for the app lifetime

## Features

### Manager
- Create, edit, and close customer profiles
- Open accounts and configure limits / interest rates
- View all transactions across any account
- Export transaction history to PDF or CSV
- Full audit trail written to `logs/` via `BankLogger`

### Customer
- Deposit, withdraw, transfer between own accounts
- View balance and full transaction history
- Daily withdrawal limit enforced at the service layer
- First-login password setup on initial access

## Database

- Oracle XE 21c, schema user: `c##banksystem`
- All queries use `PreparedStatement`
- `autoCommit = false` — commits are explicit
- Connection managed as a shared singleton in `DBConnection`

## Project Deliverables

| File | Description |
|---|---|
| `src/` | Full Java source |
| `doc/` | Javadoc (all packages) |
| `sql/SETUP.sql` | Oracle schema + seed data |
| `DIAGRAMME_CLASSE_FORTIS_BANK.drawio` | Class diagram |
| `DIAGRAMME_DUTILISATION_FORTIS_BANK.drawio` | Use-case diagram |
| `FortisBankSystems_Presentation.pptx` | Project presentation |
| `Rapport_Projet_Final_FortisBankSystems.docx` | Final report |
| `logs/` | Runtime log files |

## OOP Concepts Demonstrated

- **Encapsulation** — private fields with controlled access via getters/setters
- **Inheritance** — `Account` is abstract; four concrete account types extend it
- **Polymorphism** — `Account` references used throughout service and DAO layers
- **Interfaces** — `ITransaction` defines the contract all accounts implement
- **Custom exceptions** — domain rule violations surfaced as checked exceptions
- **Singleton** — `DBConnection`, `BankLogger`, `SessionManager`
- **DTO pattern** — clean separation between DB rows and business objects
- **DAO pattern** — all SQL isolated behind dedicated data access classes
- **Service layer** — business rules live in service classes, not in the UI

## License

MIT

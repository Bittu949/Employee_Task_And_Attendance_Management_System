# Employee Task & Attendance Management System

<p align="center">
  A secure, role-based workforce management platform for managing employees, tasks, attendance, and performance through a modern centralized dashboard.
</p>

<p align="center">
  <strong>Built with Java 21 • Spring Boot • Spring Security • Thymeleaf • Tailwind CSS • MySQL</strong>
</p>

---

## Overview

The **Employee Task & Attendance Management System (ETAMS)** is a full-stack web application designed to simplify and automate workforce management activities within an organization.

The platform provides separate role-based experiences for **Administrators** and **Employees**. Administrators can manage users, create and assign tasks, monitor task progress, track attendance, export reports, and analyze employee performance. Employees can manage their daily attendance, view assigned tasks, update task status, review task history, and manage their account information.

The application combines a secure Spring Boot backend with a clean, responsive, SaaS-inspired user interface.

---

## Key Features

### Secure Authentication & Authorization

* JWT-based authentication
* Spring Security integration
* BCrypt password hashing
* Role-based access control
* Separate ADMIN and EMPLOYEE authorization
* Secure JWT cookie handling
* Protected application routes
* Automatic admin account initialization
* Secure logout functionality

### Admin Dashboard

The Admin Dashboard provides a centralized overview of organizational activities.

* Total employee overview
* Task statistics and summaries
* Attendance overview
* Performance insights
* Quick access to management modules
* Modern responsive dashboard interface

### User Management

Administrators can manage employee and administrator accounts from a centralized interface.

* View all users
* Search employee records
* Add new users
* Edit user information
* Assign user roles
* Manage account status
* Reset user passwords
* View user information

### Task Management

The task management module supports the complete task lifecycle.

* Create new tasks
* Define task title and description
* Set task priority
* Configure task deadlines
* Assign tasks to employees
* Reassign existing tasks
* Edit task information
* Monitor task progress
* View task details
* Track task status history
* View Pending tasks
* View In Progress tasks
* View Completed tasks
* Soft-delete tasks
* Restore deleted tasks
* Permanently remove deleted tasks
* Search and filter task records
* Pagination for task listings

### Attendance Management

Employees can manage daily attendance while administrators can monitor organization-wide attendance records.

#### Employee Features

* Daily Check-In
* Daily Check-Out
* View attendance history
* View attendance statistics
* Track working hours
* View attendance status

#### Admin Features

* View all employee attendance records
* Search attendance by employee name
* Filter by date
* Filter by month
* Filter by year
* Filter by attendance status
* View Present and Absent statistics
* View attendance percentage
* Paginated attendance records
* Export filtered attendance records to CSV

### Performance Reports

The administrator can monitor employee performance using centralized performance reports.

* View employee performance records
* Analyze task completion statistics
* Monitor completed tasks
* Monitor pending tasks
* View attendance percentage
* Compare employee performance metrics

### Profile Management

Employees can manage their personal profile information.

* View profile details
* Update personal information
* View account information
* Manage profile details

### Account Settings

The application provides dedicated settings functionality for secure account management.

* Change password
* Current password verification
* New password validation
* Password confirmation
* Secure password update

---

## Application Architecture

The application follows a layered architecture that separates responsibilities across different application layers.

```text
Client / Browser
       │
       ▼
Controller Layer
       │
       ▼
Service Layer
       │
       ▼
Repository Layer
       │
       ▼
MySQL Database
```

### Architectural Principles

* Separation of concerns
* Modular package structure
* Controller-Service-Repository pattern
* Dependency injection
* Centralized exception handling
* Secure authentication and authorization
* Reusable service-layer business logic
* Maintainable and scalable application structure

---

## Technology Stack

| Layer                   | Technologies                 |
| ----------------------- | ---------------------------- |
| Backend                 | Java 21, Spring Boot         |
| Security                | Spring Security, JWT, BCrypt |
| Frontend                | Thymeleaf, HTML5, JavaScript |
| Styling                 | Tailwind CSS                 |
| Persistence             | Spring Data JPA, Hibernate   |
| Database                | MySQL                        |
| Build Tool              | Maven                        |
| API Testing             | Postman                      |
| Version Control         | Git, GitHub                  |
| Development Environment | IntelliJ IDEA                |

---

## System Roles

### Administrator

The Administrator has access to system management and reporting features.

```text
Admin
 ├── Dashboard
 ├── User Management
 │    ├── Add User
 │    ├── Edit User
 │    └── Reset Password
 │
 ├── Task Management
 │    ├── Create Task
 │    ├── Assign Task
 │    ├── Reassign Task
 │    ├── Edit Task
 │    ├── View Task Details
 │    ├── View Status History
 │    └── Restore Deleted Tasks
 │
 ├── Attendance Reports
 │    ├── Search Records
 │    ├── Filter Records
 │    └── Export CSV Report
 │
 ├── Performance Reports
 └── Account Settings
```

### Employee

Employees have access only to features related to their own work and account.

```text
Employee
 ├── Dashboard
 ├── View Assigned Tasks
 ├── Update Task Status
 ├── View Task Details
 ├── Mark Attendance
 │    ├── Check-In
 │    └── Check-Out
 │
 ├── View Attendance History
 ├── View Profile
 ├── Update Profile
 └── Account Settings
```

---

## Security

Security is a core part of the application architecture.

The application implements:

* JWT token generation after successful authentication
* JWT validation for protected requests
* Spring Security authorization rules
* Role-based endpoint protection
* BCrypt password encryption
* Secure authentication cookie handling
* Protected ADMIN resources
* Protected EMPLOYEE resources
* Password validation during account updates
* Environment-based secret configuration

> Sensitive credentials and secrets must never be committed to the repository.

---

## Environment Variables

The application requires environment variables for database credentials, JWT configuration, and initial administrator creation.

### Required Variables

| Variable         | Purpose                         |
| ---------------- | ------------------------------- |
| `DB_USERNAME`    | MySQL database username         |
| `DB_PASSWORD`    | MySQL database password         |
| `JWT_SECRET`     | Secret key used for JWT signing |
| `ADMIN_EMAIL`    | Initial administrator email     |
| `ADMIN_PASSWORD` | Initial administrator password  |
| `ADMIN_USERNAME` | Initial administrator username  |

> Use a strong JWT secret of sufficient length and never expose production credentials in source code.

---

## Environment Setup

### Windows

Open **Command Prompt** and configure the required environment variables:

```bash
setx DB_USERNAME "your_db_username"
setx DB_PASSWORD "your_db_password"
setx JWT_SECRET "your_secure_jwt_secret_key"
setx ADMIN_EMAIL "admin@example.com"
setx ADMIN_PASSWORD "your_secure_admin_password"
setx ADMIN_USERNAME "adminUser"
```

After using `setx`, restart your IDE and terminal so that the new environment variables become available to the application.

### Linux / macOS

```bash
export DB_USERNAME=your_db_username
export DB_PASSWORD=your_db_password
export JWT_SECRET=your_secure_jwt_secret_key
export ADMIN_EMAIL=admin@example.com
export ADMIN_PASSWORD=your_secure_admin_password
export ADMIN_USERNAME=adminUser
```

---

## Database Setup

### 1. Install MySQL

Ensure that a MySQL server is installed and running.

### 2. Create the Database

```sql
CREATE DATABASE ems;
```

### 3. Verify the Datasource URL

Configure the datasource URL in `application.properties` if required:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/ems
```

### 4. Environment-Based Configuration

```properties
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
jwt.secret=${JWT_SECRET}
```

---

## Getting Started

### Prerequisites

Before running the application, ensure that the following tools are installed:

* Java 21 or compatible configured JDK
* MySQL
* Git
* Maven or Maven Wrapper support
* IntelliJ IDEA or another Java IDE

### Clone the Repository

```bash
git clone https://github.com/Bittu949/employee-management-system.git
cd employee-management-system
```

### Configure Environment Variables

Set all required database, JWT, and administrator environment variables before starting the application.

### Build the Application

On Windows:

```bash
mvnw.cmd clean install
```

On Linux or macOS:

```bash
./mvnw clean install
```

### Run the Application

On Windows:

```bash
mvnw.cmd spring-boot:run
```

On Linux or macOS:

```bash
./mvnw spring-boot:run
```

### Open the Application

After successful startup, open:

```text
http://localhost:8080
```

---

## Core Business Rules

The application enforces business rules to maintain system consistency and security.

* An employee can perform attendance Check-In only according to the permitted daily attendance flow.
* Check-Out is allowed only after successful Check-In.
* Administrative functionality is restricted to ADMIN users.
* Task creation and assignment are controlled by the administrator.
* Employees can update the status of their assigned tasks.
* Task deadlines are validated.
* Duplicate user information is validated according to application rules.
* Passwords are stored securely using hashing and are never stored as plain text.
* Deleted tasks can be restored through the Restore Tasks module before permanent deletion.
* Attendance records can be searched, filtered, paginated, and exported.

---

## API Documentation

Detailed API information is maintained separately in:

```text
API_DOCUMENTATION.md
```

The API documentation contains information about application endpoints, request parameters, authentication requirements, responses, and endpoint usage.

---

## Project Structure

```text
src/
├── main/
│   ├── java/
│   │   └── com/company/ems/
│   │       ├── Config/
│   │       ├── Controller/
│   │       │   ├── Admin/
│   │       │   └── Employee/
│   │       ├── Entity/
│   │       ├── Exception/
│   │       ├── Repository/
│   │       ├── Security/
│   │       └── Service/
│   │           ├── Admin/
│   │           └── Employee/
│   │
│   └── resources/
│       ├── static/
│       ├── templates/
│       │   ├── Admin_dashboard/
│       │   └── Employee_dashboard/
│       └── application.properties
│
└── test/
```

> The exact package structure may vary slightly depending on the current project organization.

---

## Application Workflow

```text
User Opens Application
          │
          ▼
       Login
          │
          ▼
 Credential Validation
          │
          ▼
   JWT Authentication
          │
          ▼
     Role Validation
        /       \
       /         \
      ▼           ▼
 Admin Module   Employee Module
      │              │
      ▼              ▼
Management &     Tasks, Attendance
Reporting        & Profile
      \              /
       \            /
            ▼
          Logout
```

---

## UI & User Experience

The application interface follows a modern SaaS-inspired design approach.

Key UI characteristics include:

* Responsive sidebar navigation
* Role-specific dashboards
* Compact information cards
* Search and filtering controls
* Status badges
* Responsive data tables
* Paginated records
* Task management cards
* Clear validation feedback
* Confirmation dialogs for important actions
* Clean forms and account settings pages
* Mobile-responsive layouts

---

## Testing

The system has been tested using functional test cases covering both successful and failure scenarios.

Testing areas include:

* Valid and invalid login
* Role-based access control
* Unauthorized route access
* User registration validation
* Task creation validation
* Task assignment
* Task status updates
* Attendance Check-In and Check-Out rules
* Attendance filtering
* Profile updates
* Password changes
* Empty field validation
* Deadline validation

Test cases document:

```text
Test Case ID
Test Description
Test Steps
Expected Result
Actual Result
Status
```

---

## Future Enhancements

Potential future improvements include:

* Email notifications for task assignments and deadlines
* Leave management
* Advanced performance analytics
* Downloadable PDF reports
* Real-time notifications
* Multi-department management
* Audit logging
* Cloud deployment
* Automated attendance reminders
* Advanced dashboard analytics

---

## Author

**Balkrishna Naik**

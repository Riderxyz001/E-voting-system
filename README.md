# 🗳️ E-Voting System

A secure and modern **E-Voting System** built using **Spring Boot** that allows administrators to manage elections and users to participate in online voting securely.

---

## 🚀 Features

- 👤 User Authentication & Authorization
- 🔐 Spring Security Integration
- 🗳️ Create & Manage Elections
- 👥 Candidate Management
- ✅ Secure Vote Casting
- 📊 Election Results
- 🗄️ MySQL Database Integration
- 🌐 Thymeleaf-based Responsive UI

---

## 🛠️ Tech Stack

| Technology | Used |
|------------|------|
| Java | 21 |
| Spring Boot | 3.5.14 |
| Spring Security | ✅ |
| Spring Data JPA | ✅ |
| Hibernate | ✅ |
| Maven | ✅ |
| Thymeleaf | ✅ |
| MySQL | ✅ |
| Lombok | ✅ |

---

## 📂 Project Structure

```
src
 ├── main
 │   ├── java
 │   │     └── com.evoting
 │   ├── resources
 │   │     ├── static
 │   │     ├── templates
 │   │     └── application.properties
 │   └── ...
 └── test
```

---

## ⚙️ Prerequisites

Before running the project, make sure you have:

- Java 21
- Maven 3.9+
- MySQL
- IntelliJ IDEA (Recommended)

---

## 🔧 Installation

### 1. Clone the Repository

```bash
git clone https://github.com/Riderxyz001/E-voting-system.git
```

### 2. Open the Project

Open the project in IntelliJ IDEA.

### 3. Configure Database

Update `application.properties` according to your local MySQL configuration.

Example:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/evoting
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD
```

### 4. Build the Project

```bash
mvn clean install
```

### 5. Run the Application

```bash
mvn spring-boot:run
```

Open:

```
http://localhost:8080
```

---

## 📸 Screenshots

> Screenshots will be added soon.

- Home Page
- Login Page
- Admin Dashboard
- Election Management
- Voting Page
- Results Page

---

## 📌 Future Improvements

- Email Verification
- OTP Authentication
- JWT Authentication
- Docker Support
- REST API
- Unit & Integration Testing
- Cloud Deployment

---

## 🤝 Contributing

Contributions, issues, and feature requests are welcome.

Feel free to fork this repository and submit a pull request.

---

## 👨‍💻 Author

**Ajay Yadav**

- GitHub: https://github.com/Riderxyz001
- LinkedIn: https://www.linkedin.com/in/ajay-yadav-04997a324

---

## ⭐ Support

If you found this project useful, consider giving it a **⭐ Star** on GitHub.

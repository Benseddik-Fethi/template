# 🚀 Spring Boot Template - Production Ready

Template Spring Boot générique avec OAuth2, RustFS, et architecture clean.

## 📦 Stack Technique

- **Java 21**
- **Spring Boot 3.5.6**
- **PostgreSQL 16**
- **Keycloak** (OAuth2 / JWT)
- **RustFS** (S3-compatible)
- **Liquibase** (DB migrations)
- **SpringDoc OpenAPI** (Swagger)

## 🏁 Démarrage Rapide

### 1. Prérequis

- Java 21+
- Maven 3.8+
- PostgreSQL 16
- Keycloak 23+

### 2. Configuration
```bash
# Copier le fichier d'exemple
cp .env.example .env

# Éditer avec vos valeurs
nano .env
```

### 3. Démarrer l'application
```bash
# Avec Maven wrapper
./mvnw spring-boot:run

# Ou avec Maven installé
mvn spring-boot:run
```

### 4. Accéder à l'application

- **API** : http://localhost:8080/api/v1
- **Swagger** : http://localhost:8080/api/v1/swagger-ui.html
- **Health** : http://localhost:8080/api/v1/health

## 🏗️ Architecture
```
Controller → Service → Repository
```

### Structure du Projet
```
src/main/java/com/benseddik/template/
├── web/                    # Controllers (HTTP)
│   ├── AuthController.java
│   ├── UserController.java
│   └── ImageController.java
├── service/                # Services (Logique métier)
│   ├── KeycloakService.java
│   ├── UserService.java
│   ├── RustFsService.java
│   └── CurrentUserService.java
├── repository/             # Repositories (Données)
│   └── AppUserRepository.java
├── domain/                 # Entités JPA
│   ├── AppUser.java
│   └── AbstractAuditingEntity.java
├── config/                 # Configuration
│   ├── SecurityConfig.java
│   ├── KeycloakConfig.java
│   └── RustFsConfig.java
├── security/               # Sécurité
│   └── CurrentUserService.java
└── error/                  # Gestion d'erreurs
    ├── ExceptionsHandler.java
    └── record/
        └── ErrorResponse.java
```

## 🔒 Sécurité

- OAuth2 Resource Server (Keycloak)
- JWT validation avec multi-issuer
- JPA Auditing automatique
- Gestion d'erreurs centralisée (RFC 7807)
- Headers de sécurité (CSP, HSTS, CORS)

## 📚 Endpoints Principaux

### Publics

- `GET /health` - Health check
- `GET /swagger-ui.html` - Documentation API
- `POST /auth/register` - Inscription

### Protégés (JWT requis)

- `GET /users/me` - Profil utilisateur
- `PUT /users/me` - Modifier profil
- `DELETE /users/me` - Supprimer compte
- `POST /images/users` - Upload photo profil
- `DELETE /images/{folder}/{file}` - Supprimer image

## 🔧 Configuration

### Variables d'Environnement Obligatoires

| Variable | Description | Exemple |
|----------|-------------|---------|
| `DATABASE_URL` | URL PostgreSQL | `jdbc:postgresql://localhost:5432/myapp` |
| `DATABASE_USERNAME` | User DB | `postgres` |
| `DATABASE_PASSWORD` | Password DB | `secret` |
| `KEYCLOAK_REALM` | Realm Keycloak | `my-realm` |
| `KEYCLOAK_CLIENT_ID` | Client ID | `my-client` |
| `RUSTFS_BUCKET_NAME` | Nom du bucket | `my-bucket` |

Voir `.env.example` pour toutes les variables.

## 🧪 Tests
```bash
# Tests unitaires
mvn test

# Tests d'intégration
mvn verify

# Note: JaCoCo pour la couverture de code sera ajouté dans une future version
```

## 🚀 Déploiement

### Docker
```bash
# TODO: Dockerfile à créer
# Le template inclura un Dockerfile optimisé multi-stage dans une future version
```

### Production

1. Configurer les variables d'environnement production
2. Builder : `mvn clean package -DskipTests`
3. Déployer le JAR : `target/*.jar`

## 🛠️ Développement

### Ajouter un Nouveau Endpoint

1. Créer le Service
2. Créer le Controller qui utilise le Service
3. Ajouter les tests

**Exemple :**
```java
// 1. Service
@Service
@RequiredArgsConstructor
public class DocumentService {
    private final DocumentRepository repository;
    
    public Document create(CreateDocumentRequest req) {
        // Logique métier
    }
}

// 2. Controller
@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentService documentService;
    
    @PostMapping
    public ResponseEntity<Document> create(@RequestBody CreateDocumentRequest req) {
        return ResponseEntity.ok(documentService.create(req));
    }
}
```

## 📖 Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [API Documentation](http://localhost:8080/api/v1/swagger-ui.html)

## 🐛 Dépannage

### L'application ne démarre pas
```bash
# Vérifier que PostgreSQL tourne
docker ps | grep postgres

# Vérifier que Keycloak tourne
curl http://localhost:8081/health

# Vérifier les logs
tail -f logs/template.log
```

### Erreur 401 sur tous les endpoints

- Vérifier que Keycloak est accessible
- Vérifier `KEYCLOAK_REALM` dans `.env`
- Vérifier que le realm existe dans Keycloak

### Erreur de connexion RustFS

- Vérifier `RUSTFS_ACCESS_KEY` et `RUSTFS_SECRET_KEY`
- Vérifier que le bucket existe

## 🤝 Contribution

1. Fork le projet
2. Créer une branche (`git checkout -b feature/amazing`)
3. Commit (`git commit -m 'Add amazing feature'`)
4. Push (`git push origin feature/amazing`)
5. Ouvrir une Pull Request

## 📝 License

MIT License

## 👤 Auteur

Fethi Benseddik
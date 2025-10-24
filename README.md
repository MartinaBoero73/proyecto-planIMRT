# Proyecto-planIMRT

Proyecto Integrador de Ingeniería de Software - Licenciatura en Bioinformática - FIUNER
Basado en el Proyecto Final de grado de Laura Osuna.

## Tabla de Contenidos

- [Descripción](#descripción)
- [Equipo](#equipo)
- [Estructura](#estructura-del-proyecto)

## Descripción

En la Radioterapia de Intensidad Modulada (IMRT) hay una alta cantidad de campos de tratamiento que no pasan el control de calidad. Esto implica que el físico médico a cargo de la planificación de tratamiento debe realizar nuevamente la planificación y problarlo. Es por ello que se propone un sistema que, a partir de el archivo con la planificación, calcule métricas que ayuden al usuario a tomar decisiones sobre el plan de tratamiento.

## Equipo

- [MartinaBoero73](https://github.com/MartinaBoero73) — Responsable del Repositorio
- [IvanaGasco](https://github.com/IvanaGasco) — Colaboradora
- [CMacchi25](https://github.com/CMacchi25) — Colaboradora

## Estructura del proyecto

```plaintext
proyecto-planIMRT/
│
├── planIMRT/                           # Implementación principal del sistema (aplicación Spring Boot)
│   └── src/
|       ├── main/
|       |   ├── java/com.planimrt/
|       |   |   ├── PlanImrtApplication.java    # Aplicacion principal del proyecto
|       |   |   ├── controllers/                # Controladores REST para manejo de peticiones
|       |   |   ├── DTOs/                       # Objetos de transferencia de datos entre capas 
|       |   |   ├── forms/                      # Clases para manejar formularios o requests del usuario 
|       |   |   ├── model/                      # Entidades del dominio 
|       |   |   ├── repo/                       # Interfaces de repositorio para acceso a la base de datos
|       |   |   ├── security/                   # Configuración de autenticación/autorización 
|       |   |   └── services/                   # Lógica de negocio
|       |   |
|       |   └── resources/
|       |       ├── templates/            # Vistas HTML 
|       |       └── static/               # Archivos estáticoservidos directamente
|       |
|       └── test/                        # Tests unitarios e integrados del proyecto 
|
├── documentacion/                      # Documentación formal del proyecto
    ├── especificaciones/               # Especificaciones técnicas y funcionales
    └── requerimientos/                 # Requerimientos del sistema
        ├── casos_de_uso.xlsx           # Lista de casos de uso en formato tabular
        └── escenarios_de_calidad.xlsx  # Escenarios de atributos de calidad
 
 
```

## Elementos de Configuración

Total de CIs: 6

- Documentación:  2 CIs
- Diseño: 4 CIs

## Última actualización

1/9/2025 - v2.0

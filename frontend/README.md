# Intelligent Patient Flow & Bed Capacity Orchestration System — Frontend

A lightweight, modern React 19 Single Page Application built with Vite, Tailwind CSS v4, official **shadcn/ui** components, and the **TanStack** suite.

## Tech Stack

- **React 19** (`react@19.2.8`)
- **TanStack Ecosystem**:
  - `@tanstack/react-router`: Type-safe route hierarchy with automatic layout nesting.
  - `@tanstack/react-query`: Asynchronous state management with live background polling (2.5–3s intervals) and reactive cache invalidation.
  - `@tanstack/react-table`: Headless table architecture for clinical queues and bed inventories.
- **UI & Styling**:
  - **Tailwind CSS v4** (`@tailwindcss/vite`)
  - **shadcn/ui** official components (`Button`, `Card`, `Badge`, `Tabs`, `Dialog`, `Select`, `Slider`, `Input`, `Table`, `Progress`)
  - `lucide-react`: Clinical and operational iconography
- **Build Tool**: Vite 8

## Application Screens & Routes

| Route | Role Persona | Description |
| :--- | :--- | :--- |
| `/ed` | 🩺 ED Attending (`dr_tan_ed`) | Automated diagnostic synthesis with 1-click clinical directives submission and parallel specialist broadcast. |
| `/specialist` | 👨‍⚕️ Inpatient Specialist (`dr_lim_cardio`) | Asynchronous consult feed with case claim lock, consult impression entry, and clinical discordance alerts. |
| `/bmu` | 🏢 BMU Coordinator (`bmu_coord_wong`) | Priority admission queue, algorithmic Top-3 bed recommendations with score breakdowns, 1-click allocation, sister hospital diversion, and 4-state live bed inventory matrix. |
| `/bmu/config` | 🏢 BMU Coordinator | Interactive sliders tuning specialty alignment, consolidation packing, fall-risk proximity, and batch thresholds. |
| `/ward` | 👩‍⚕️ Ward Nurse / 🧹 EVS Housekeeping | Ward nursing station bed controls (check-in / vacate) and 30-minute terminal sanitization sign-off queue. |
| `/patient` | 📱 Patient & Family | Mobile smartphone frame simulator displaying real-time 4-stage journey milestones, estimated wait time, pax ahead in queue, operational delay reasons, and financial/care guidance. |

## Quick Start

### 1. Run the Spring Boot Backend (Port 8080)
```bash
cd backend
./mvnw spring-boot:run
```

### 2. Run the Frontend Dev Server (Port 5173)
```bash
cd frontend
npm run dev
```

Open `http://localhost:5173` in your browser. All `/api` requests are automatically proxied to `http://localhost:8080`.

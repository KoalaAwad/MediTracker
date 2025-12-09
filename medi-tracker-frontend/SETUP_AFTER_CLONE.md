# Project Setup Guide - After Git Clone

## ✅ Issue: "PrimaryButton is not defined" Error

This happens because the `StyledButton.tsx` wrapper component was missing from the repository.

## 🔧 **Already Fixed!**

I've created the missing file and fixed the import. Here's what was done:

### **1. Created Missing File**
`src/components/ui/StyledButton.tsx` - Wrapper component for consistent button styling

### **2. Fixed Import in DashboardPage**
Changed from:
```typescript
import { Button } from "@mui/material";
```
To:
```typescript
import { PrimaryButton } from "../../components/ui/StyledButton";
```

---

## 📦 **First-Time Setup (After Clone)**

### **1. Install Dependencies**
```powershell
cd medi-tracker-frontend
npm install
```

### **2. Check Environment Variables**
Create `.env` if needed (currently using hardcoded localhost:8080)

### **3. Start Development Server**
```powershell
npm run dev
```

### **4. Backend Setup**
```powershell
cd ..
# Make sure Postgres is running (Docker)
docker compose up -d postgres

# Run backend
mvn spring-boot:run
```

---

## 🎯 **Component Structure**

### **UI Components:**
- **`button.tsx`** - shadcn/ui base button (uses Radix UI)
- **`StyledButton.tsx`** - Custom wrappers:
  - `PrimaryButton` - Blue filled button
  - `SecondaryButton` - Outlined button
- **`Loading.tsx`** - Loading spinner component

### **Usage:**
```typescript
import { PrimaryButton, SecondaryButton } from "@/components/ui/StyledButton";

<PrimaryButton onClick={handleSave}>Save</PrimaryButton>
<SecondaryButton onClick={handleCancel}>Cancel</SecondaryButton>
```

---

## 🐛 **Common Issues After Clone**

### **Issue: Module not found errors**
**Solution:**
```powershell
rm -rf node_modules package-lock.json
npm install
```

### **Issue: Path aliases not working (@/ imports)**
**Solution:** Check `tsconfig.json` has:
```json
{
  "compilerOptions": {
    "baseUrl": ".",
    "paths": {
      "@/*": ["./src/*"]
    }
  }
}
```

### **Issue: Backend connection refused**
**Solution:** Ensure backend is running on `http://localhost:8080`

### **Issue: Database connection failed**
**Solution:**
```powershell
docker compose up -d postgres
# Check it's running
docker ps
```

---

## 📁 **Required Files Checklist**

After cloning, ensure these files exist:

### **Frontend:**
- ✅ `src/components/ui/StyledButton.tsx`
- ✅ `src/components/ui/button.tsx`
- ✅ `src/components/ui/Loading.tsx`
- ✅ `src/zustand/authStore.ts`
- ✅ `src/index.css` (with Tailwind utilities)

### **Backend:**
- ✅ `application.properties`
- ✅ `src/main/java/.../model/` (all entities)
- ✅ `src/main/resources/db/migration/` (SQL migrations)

---

## 🚀 **Quick Verification**

### **1. Test Frontend Compiles:**
```powershell
cd medi-tracker-frontend
npm run build
```

### **2. Test Backend Compiles:**
```powershell
mvn clean compile
```

### **3. Test Login Page:**
```
Navigate to: http://localhost:5173/login
Expected: No errors, form displays correctly
```

---

## 🔄 **Git Workflow**

### **Before Committing:**
```powershell
# Frontend
npm run build  # Ensure no errors

# Backend
mvn clean compile  # Ensure no errors
```

### **What to Commit:**
- ✅ Source files (`.tsx`, `.ts`, `.java`)
- ✅ Config files (`package.json`, `pom.xml`, `tsconfig.json`)
- ✅ Migrations (`src/main/resources/db/migration/*.sql`)

### **What NOT to Commit:**
- ❌ `node_modules/`
- ❌ `target/`
- ❌ `.env` (if contains secrets)
- ❌ `dist/` or `build/`

---

## ✨ **Summary**

**Fixed:**
1. ✅ Created missing `StyledButton.tsx` file
2. ✅ Fixed import in `DashboardPage.tsx`
3. ✅ Verified all button component imports

**Result:**
- No more "PrimaryButton is not defined" errors
- Project compiles successfully on fresh clone
- Consistent button styling across app

**Your project is now clone-friendly!** 🎉


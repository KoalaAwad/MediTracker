# Add Prescription UI Improvements

## ✅ Changes Made

### 1. **Fixed Date Field Label Overlap**
**Problem:** "Start date" and "End date" labels were overlapping with typed dates

**Solution:** Added `InputLabelProps={{ shrink: true }}` to date fields
```tsx
<TextField
  label="Start date"
  type="date"
  InputLabelProps={{ shrink: true }}  // ← Keeps label above field
  // ...
/>
```

### 2. **Added "Daily" Toggle Feature**
**Problem:** Patients had to manually add 7 entries for daily medications

**Solution:** Added checkbox for each schedule entry
- ☑️ **Daily checked** → Automatically creates entries for all 7 days
- ☐ **Daily unchecked** → Single entry for selected day

---

## 🎯 How "Daily" Works

### **Example Use Case:**
Patient takes medicine at 10 AM and 10 PM every day

**Old way (tedious):**
1. Add entry: MONDAY 10:00
2. Add entry: TUESDAY 10:00
3. Add entry: WEDNESDAY 10:00
4. ... (14 total entries for 2 times × 7 days)

**New way (simple):**
1. Add entry: 10:00 → Check "Daily" ✓
2. Add entry: 22:00 → Check "Daily" ✓
3. Submit → Backend receives 14 entries automatically!

---

## 📊 Database Schema - Prescription Schedule

### **Your Question:** 
> "What field in the prescriptions database stores the information of how many times a week a medicine is taken?"

### **Answer:**
**No single field** - it's derived from the `prescription_schedule` table.

### **Database Structure:**
```
prescription
  ├─ prescription_id (PK)
  ├─ patient_id
  ├─ medicine_id
  ├─ dosage_amount
  ├─ dosage_unit
  ├─ start_date
  ├─ end_date
  └─ time_zone

prescription_schedule (separate table)
  ├─ prescription_id (FK)
  ├─ day_of_week (e.g., "MONDAY")
  └─ time_of_day (e.g., "10:00")
  
  Primary Key: (prescription_id, day_of_week, time_of_day)
```

### **Example Data:**

**Prescription Table:**
```
prescription_id | medicine_id | patient_id | dosage_amount | dosage_unit
1              | 5           | 10         | 1             | TABLET
```

**Prescription Schedule Table:**
```
prescription_id | day_of_week | time_of_day
1              | MONDAY      | 10:00
1              | MONDAY      | 22:00
1              | TUESDAY     | 10:00
1              | TUESDAY     | 22:00
1              | WEDNESDAY   | 10:00
1              | WEDNESDAY   | 22:00
... (all 7 days)
```

### **Calculating Times Per Week:**
```sql
-- Count total doses per week for a prescription
SELECT prescription_id, COUNT(*) as doses_per_week
FROM prescription_schedule
WHERE prescription_id = 1
GROUP BY prescription_id;
-- Result: 14 (2 times × 7 days)

-- Count unique days per week
SELECT prescription_id, COUNT(DISTINCT day_of_week) as days_per_week
FROM prescription_schedule
WHERE prescription_id = 1
GROUP BY prescription_id;
-- Result: 7 (every day)

-- Count times per day
SELECT prescription_id, day_of_week, COUNT(*) as times_per_day
FROM prescription_schedule
WHERE prescription_id = 1
GROUP BY prescription_id, day_of_week;
-- Result: Each day has 2 doses
```

---

## 🎨 UI Changes Summary

### **Before:**
```
[Day of Week ▼] [Time ___:___] [Remove]
```
- Label overlaps with date input ❌
- Must add 7 entries for daily medication ❌

### **After:**
```
Start date: [____/____/____]  ← Label always above ✓
End date:   [____/____/____]  ← Label always above ✓

[Day of Week ▼] [Time ___:___] ☐ Daily [Remove]
                                 ↑ New checkbox!
```
- When "Daily" checked: Day picker hides, will create 7 entries ✓
- When "Daily" unchecked: Normal single-day entry ✓

---

## 🧪 Testing

### Test 1: Date Labels
1. Click on start date field
2. **Expected:** Label stays above, doesn't overlap

### Test 2: Daily Toggle - Visual
1. Add a schedule entry
2. Check "Daily"
3. **Expected:** Day of week picker disappears
4. Uncheck "Daily"
5. **Expected:** Day of week picker reappears

### Test 3: Daily Toggle - Functionality
1. Set time to 10:00
2. Check "Daily"
3. Click "Save prescription"
4. View database
5. **Expected:** 7 entries (one for each day at 10:00)

### Test 4: Multiple Times Daily
1. Add entry: 08:00, check "Daily"
2. Add entry: 20:00, check "Daily"
3. Save
4. **Expected:** 14 entries in database (2 × 7)

### Test 5: Mixed Schedule
1. Add entry: MONDAY 10:00 (not daily)
2. Add entry: 20:00, check "Daily"
3. Save
4. **Expected:** 8 entries (1 for Monday + 7 for daily 20:00)

---

## 💡 Implementation Details

### **State Management:**
```tsx
const [schedule, setSchedule] = useState<ScheduleEntryDto[]>([...]);
const [dailyFlags, setDailyFlags] = useState<boolean[]>([false]);
```

### **Expansion Logic (on submit):**
```tsx
schedule.forEach((entry, index) => {
  if (dailyFlags[index]) {
    // Daily: add for all 7 days
    allDays.forEach(day => {
      expandedSchedule.push({ dayOfWeek: day, timeOfDay: entry.timeOfDay });
    });
  } else {
    // Not daily: single entry
    expandedSchedule.push(entry);
  }
});
```

---

## 🔮 Future Enhancements

### 1. **Preset Patterns**
Add buttons for common schedules:
- "Once Daily" → 1 time at chosen hour
- "Twice Daily" → 08:00 and 20:00
- "Three Times Daily" → 08:00, 14:00, 20:00
- "Every 6 Hours" → 00:00, 06:00, 12:00, 18:00

### 2. **Smart Defaults**
Auto-suggest common medication times:
- Morning: 08:00
- Afternoon: 14:00
- Evening: 20:00
- Bedtime: 22:00

### 3. **Visual Schedule Preview**
Show a weekly calendar grid before submitting:
```
        Mon  Tue  Wed  Thu  Fri  Sat  Sun
10:00   ✓    ✓    ✓    ✓    ✓    ✓    ✓
22:00   ✓    ✓    ✓    ✓    ✓    ✓    ✓
```

### 4. **Custom Days**
Add "Weekdays Only" or "Weekends Only" toggles:
- ☐ Weekdays (Mon-Fri)
- ☐ Weekends (Sat-Sun)

---

## 📝 Summary

✅ **Date labels fixed** - No more overlap  
✅ **Daily toggle added** - Easy to add daily medications  
✅ **Schedule stored in separate table** - Flexible, normalized design  
✅ **Times per week calculated** - Query the schedule table  

**Result:** Much better UX for patients adding prescriptions! 🎉


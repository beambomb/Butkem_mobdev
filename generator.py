import time
import requests
import random
import datetime
import math

# ==========================================
# KONFIGURASI SUPABASE
# ==========================================
SUPABASE_URL = "https://jmfawibrlwiebqdyhdsk.supabase.co/rest/v1/energy_logs"
SUPABASE_KEY = "sb_publishable_260CCSS5agIPHgDAKh04JA_u3Mps_Ho"

HEADERS = {
    "apikey": SUPABASE_KEY,
    "Authorization": f"Bearer {SUPABASE_KEY}",
    "Content-Type": "application/json",
    "Prefer": "return=minimal"
}

def send_to_supabase(payload):
    try:
        response = requests.post(SUPABASE_URL, json=payload, headers=HEADERS)
        if response.status_code in [200, 201]:
            print(f"[{datetime.datetime.now().strftime('%H:%M:%S')}] {payload['lini_name']} | {payload['power_kw']} kW")
        else:
            print(f"[ERROR] Gagal kirim {payload['lini_name']}: {response.text}")
    except Exception as e:
        print(f"[ERROR] Koneksi bermasalah: {e}")

def main():
    print("[START] Memulai Mesin Simulasi 4 Shop Otomotif (AEMADS)...")
    print("Mengirimkan data paralel untuk 4 Shop setiap 2 detik...\n")
    
    counter = 0
    body_drift_count = 0
    
    while True:
        payloads = []
        
        # ----------------------------------------------------
        # SINE WAVE LOGIC FOR VISUAL "HILLS AND VALLEYS"
        # ----------------------------------------------------
        t = counter / 2.0
        
        # ----------------------------------------------------
        # SHOP 1: STAMPING & PRESS (Base: 50 kW)
        # Rentan terjadi Spike Anomaly (Case 2) akibat hentakan mesin press
        # ----------------------------------------------------
        s1_power = round(50.0 + (math.sin(t) * 15.0) + random.uniform(-2.0, 2.0), 2)
        s1_pf = round(random.uniform(0.85, 0.90), 2)
        s1_oee = round(random.uniform(85.0, 95.0), 1)
        s1_thd = round(random.uniform(4.0, 6.0), 1)
        
        if counter > 0 and counter % 10 == 0:
            print("[ALARM] [STAMPING] SPIKE ANOMALY TERDETEKSI!")
            s1_power = round(random.uniform(200.0, 250.0), 2)
            s1_pf = round(random.uniform(0.40, 0.50), 2)
            s1_oee = round(random.uniform(30.0, 40.0), 1)
            
        payloads.append({
            "lini_name": "Shop 1 - Stamping & Press",
            "power_kw": s1_power, "power_factor": s1_pf, "output_qty": 10,
            "sec_val": round(s1_power/10, 2), "oee_score": s1_oee, "thd_value": s1_thd
        })
        
        # ----------------------------------------------------
        # SHOP 2: BODY & WELDING (Base: 100 kW)
        # Rentan terjadi Drift Anomaly (Case 3) dan tingginya THD akibat robot las
        # ----------------------------------------------------
        s2_power = round(100.0 + (math.cos(t * 0.8) * 20.0) + random.uniform(-3.0, 3.0), 2)
        s2_pf = round(random.uniform(0.88, 0.92), 2)  # NORMAL PF
        s2_oee = round(random.uniform(85.0, 95.0), 1)
        s2_thd = round(random.uniform(4.0, 7.0), 1)   # NORMAL THD
        
        if counter > 0 and ((counter + 6) % 16 == 0 or 0 < body_drift_count < 3):
            if (counter + 6) % 16 == 0: body_drift_count = 1
            print(f"[WARNING] [BODY WELDING] DRIFT ANOMALY (Siklus {body_drift_count}/3)")
            s2_power += (20.0 * body_drift_count)
            s2_pf = round(random.uniform(0.70, 0.82), 2)  # ANOMALY PF < 0.85
            s2_oee -= (5.0 * body_drift_count)
            s2_thd = round(random.uniform(15.0, 25.0), 1) # ANOMALY THD > 10.0
            body_drift_count += 1
            if body_drift_count >= 3: body_drift_count = 0
            
        payloads.append({
            "lini_name": "Shop 2 - Body & Welding",
            "power_kw": s2_power, "power_factor": s2_pf, "output_qty": 5,
            "sec_val": round(s2_power/5, 2), "oee_score": s2_oee, "thd_value": s2_thd
        })

        # ----------------------------------------------------
        # SHOP 3: PAINT SHOP (Base: 250 kW)
        # ----------------------------------------------------
        s3_power = round(250.0 + (math.sin(t * 1.5) * 35.0) + random.uniform(-5.0, 5.0), 2)
        s3_pf = round(random.uniform(0.92, 0.98), 2)
        s3_oee = round(random.uniform(92.0, 98.0), 1)
        s3_thd = round(random.uniform(1.5, 3.0), 1)
        
        payloads.append({
            "lini_name": "Shop 3 - Paint Shop",
            "power_kw": s3_power, "power_factor": s3_pf, "output_qty": 2,
            "sec_val": round(s3_power/2, 2), "oee_score": s3_oee, "thd_value": s3_thd
        })

        # ----------------------------------------------------
        # SHOP 4: GENERAL ASSEMBLY (Base: 50 kW)
        # ----------------------------------------------------
        s4_power = round(50.0 + (math.cos(t * 1.2) * 10.0) + random.uniform(-2.0, 2.0), 2)
        s4_pf = round(random.uniform(0.90, 0.95), 2)
        s4_oee = round(random.uniform(90.0, 96.0), 1)
        s4_thd = round(random.uniform(2.0, 4.0), 1)
        
        payloads.append({
            "lini_name": "Shop 4 - General Assembly",
            "power_kw": s4_power, "power_factor": s4_pf, "output_qty": 4,
            "sec_val": round(s4_power/4, 2), "oee_score": s4_oee, "thd_value": s4_thd
        })

        # KIRIM SEMUA PAYLOAD
        for p in payloads:
            send_to_supabase(p)
            
        print("-" * 50)
        
        counter += 2
        time.sleep(2)

if __name__ == "__main__":
    main()

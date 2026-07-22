import time
import requests
import random
import datetime

# ==========================================
# KONFIGURASI SUPABASE (ISI NANTI)
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
    if "ISI_DENGAN" in SUPABASE_URL:
        print("[WARNING] Supabase URL & Key belum diisi. Hanya print ke console.")
        print(payload)
        return
        
    try:
        response = requests.post(SUPABASE_URL, json=payload, headers=HEADERS)
        if response.status_code in [200, 201]:
            print(f"[{datetime.datetime.now().strftime('%H:%M:%S')}] Sukses kirim data: {payload['power_kw']} kW")
        else:
            print(f"[ERROR] Gagal kirim: {response.text}")
    except Exception as e:
        print(f"[ERROR] Koneksi bermasalah: {e}")

def main():
    print("[START] Memulai Mesin Simulasi Pabrik Otomotif (AEMADS)...")
    print("Menunggu data dikirim setiap 2 detik...\n")
    
    counter = 0
    drift_cycle_count = 0
    
    while True:
        # BASELINE CASE 1 (Normal)
        power_kw = round(random.uniform(80.0, 110.0), 2)
        power_factor = round(random.uniform(0.88, 0.94), 2)
        output_qty = 5
        oee_score = round(random.uniform(85.0, 95.0), 1)
        thd_value = round(random.uniform(2.0, 4.5), 1)
        lini_name = "Lini Assembly"
        
        # Cek Case 2: Spike Anomaly (Kelipatan 10)
        if counter > 0 and counter % 10 == 0:
            print("[ALARM] [CASE 2] SPIKE ANOMALY TERDETEKSI!")
            power_kw = round(random.uniform(270.0, 290.0), 2)
            power_factor = round(random.uniform(0.40, 0.48), 2)
            output_qty = 0
            oee_score = round(random.uniform(20.0, 30.0), 1) # OEE anjlok
            lini_name = "Lini Stamping"
            
        # Cek Case 3: Drift Anomaly (Kelipatan 13, bertahan 3 siklus)
        if counter > 0 and (counter % 13 == 0 or 0 < drift_cycle_count < 3):
            if counter % 13 == 0:
                drift_cycle_count = 1
                
            print(f"[WARNING] [CASE 3] DRIFT ANOMALY (Siklus {drift_cycle_count}/3)")
            # Degradasi linier bertambah
            power_kw = power_kw + (15.0 * drift_cycle_count)
            power_factor = power_factor - (0.04 * drift_cycle_count)
            oee_score = oee_score - (5.0 * drift_cycle_count)
            thd_value = round(random.uniform(15.0, 25.0), 1) # Harmonisa melonjak tajam
            lini_name = "Lini Welding"
            
            drift_cycle_count += 1
            if drift_cycle_count >= 3:
                drift_cycle_count = 0 # Reset setelah 3 siklus

        # Kalkulasi Specific Energy Consumption (SEC)
        sec_val = round((power_kw / output_qty) if output_qty > 0 else power_kw, 2)
        
        # Susun Payload
        payload = {
            "lini_name": lini_name,
            "power_kw": power_kw,
            "power_factor": power_factor,
            "output_qty": output_qty,
            "sec_val": sec_val,
            "oee_score": oee_score,
            "thd_value": thd_value
        }
        
        send_to_supabase(payload)
        
        counter += 2 # Karena sleep 2 detik
        time.sleep(2)

if __name__ == "__main__":
    main()

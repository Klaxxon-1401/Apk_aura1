#!/usr/bin/env python3
"""
Script to download and process probonopd/irdb database for bundling into Android app assets.

This script:
1. Downloads the IRDB repository from GitHub
2. Converts CSV files to a format suitable for the app
3. Optionally creates a JSON backup format

Usage:
    python download_irdb.py [--output-dir assets] [--format csv|json|both]
"""

import os
import sys
import csv
import json
import argparse
import urllib.request
import tempfile
import zipfile
from pathlib import Path

IRDB_REPO_URL = "https://github.com/probonopd/irdb/archive/refs/heads/master.zip"
IRDB_CSV_PATH = "irdb-master/csv"

def download_irdb(output_dir: Path):
    """Download IRDB repository and extract CSV files"""
    print("Downloading IRDB repository...")
    
    with tempfile.NamedTemporaryFile(suffix='.zip', delete=False) as tmp_file:
        urllib.request.urlretrieve(IRDB_REPO_URL, tmp_file.name)
        
        with zipfile.ZipFile(tmp_file.name, 'r') as zip_ref:
            # Extract only CSV files
            csv_files = [f for f in zip_ref.namelist() if f.endswith('.csv') and 'csv/' in f]
            
            if not csv_files:
                print("Warning: No CSV files found in IRDB repository")
                return False
            
            # Create output directory
            output_dir.mkdir(parents=True, exist_ok=True)
            
            # Extract and merge CSV files
            merged_data = []
            header_written = False
            
            for csv_file in csv_files:
                print(f"Processing {csv_file}...")
                with zip_ref.open(csv_file) as f:
                    reader = csv.reader(f.read().decode('utf-8').splitlines())
                    rows = list(reader)
                    
                    if rows:
                        # Skip header if already written
                        if not header_written:
                            merged_data.append(rows[0])
                            header_written = True
                        
                        # Add data rows
                        merged_data.extend(rows[1:])
        
        os.unlink(tmp_file.name)
    
    return merged_data

def save_as_csv(data, output_path: Path):
    """Save data as CSV file"""
    print(f"Saving CSV to {output_path}...")
    with open(output_path, 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        writer.writerows(data)
    print(f"Saved {len(data)} rows to {output_path}")

def save_as_json(data, output_path: Path):
    """Convert CSV data to JSON format"""
    print(f"Saving JSON to {output_path}...")
    
    if not data:
        return
    
    header = data[0]
    json_data = {}
    
    for row in data[1:]:
        if len(row) < 3:
            continue
        
        brand = row[0] if len(row) > 0 else "Unknown"
        device = row[1] if len(row) > 1 else "Unknown"
        function = row[2] if len(row) > 2 else "Unknown"
        protocol = row[3] if len(row) > 3 else "Unknown"
        pronto_hex = row[4] if len(row) > 4 else ""
        
        if brand not in json_data:
            json_data[brand] = []
        
        json_data[brand].append({
            "device": device,
            "protocol": protocol,
            "function": function,
            "prontoHex": pronto_hex
        })
    
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(json_data, f, indent=2, ensure_ascii=False)
    
    print(f"Saved {len(json_data)} brands to {output_path}")

def main():
    parser = argparse.ArgumentParser(description='Download and process IRDB database')
    parser.add_argument('--output-dir', default='assets', help='Output directory (default: assets)')
    parser.add_argument('--format', choices=['csv', 'json', 'both'], default='both',
                       help='Output format (default: both)')
    
    args = parser.parse_args()
    
    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)
    
    # Download and process IRDB
    data = download_irdb(output_dir)
    
    if not data:
        print("Error: Failed to download or process IRDB data")
        sys.exit(1)
    
    # Save in requested format(s)
    if args.format in ['csv', 'both']:
        save_as_csv(data, output_dir / 'irdb.csv')
    
    if args.format in ['json', 'both']:
        save_as_json(data, output_dir / 'irdb.json')
    
    print(f"\nDone! Processed {len(data)} rows from IRDB")
    print(f"Output directory: {output_dir.absolute()}")

if __name__ == '__main__':
    main()


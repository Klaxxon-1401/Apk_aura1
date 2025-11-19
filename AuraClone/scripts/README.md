# IRDB Data Processing Scripts

## download_irdb.py

Downloads the probonopd/irdb database from GitHub and converts it to a format suitable for the Android app.

### Usage

```bash
# Download and convert to both CSV and JSON
python download_irdb.py

# Only CSV format
python download_irdb.py --format csv

# Only JSON format
python download_irdb.py --format json

# Custom output directory
python download_irdb.py --output-dir app/src/main/assets
```

### Output

- `irdb.csv`: CSV format compatible with OpenCSV library
- `irdb.json`: JSON format for legacy compatibility

### Requirements

- Python 3.6+
- Internet connection (to download from GitHub)

### Notes

The script downloads the entire IRDB repository, which can be large. It extracts and merges all CSV files into a single file for easier processing in the Android app.

After running the script, copy the generated files to `app/src/main/assets/` directory.


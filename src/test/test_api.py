#!/usr/bin/env python3
"""
MediTracker API Testing Script
Usage: python test_api.py [command] [options]
"""

import requests
import json
import argparse
import sys
from pathlib import Path

# API Configuration
BASE_URL = "http://localhost:8080/api/medicines"
#BASE_URL = "http://localhost:8080/api/medicines-schedule"


def load_json(file_path):
    """Load JSON data from file"""
    try:
        with open(file_path, 'r') as f:
            return json.load(f)
    except FileNotFoundError:
        print(f"❌ File not found: {file_path}")
        return None
    except json.JSONDecodeError as e:
        print(f"❌ Invalid JSON in {file_path}: {e}")
        return None

def pretty_print_response(response):
    """Pretty print API response"""
    print(f"Status: {response.status_code} {response.reason}")
    print(f"URL: {response.url}")
    
    if response.headers.get('content-type', '').startswith('application/json'):
        try:
            data = response.json()
            print("Response:")
            print(json.dumps(data, indent=2))
        except json.JSONDecodeError:
            print("Response (not JSON):")
            print(response.text)
    else:
        print("Response:")
        print(response.text if response.text else "(Empty)")

def create_medicine(json_file="medicine.json"):
    """Create a new medicine from JSON file"""
    print(f"🏥 Creating medicine from {json_file}...")
    
    data = load_json(json_file)
    if not data:
        return
    
    try:
        response = requests.post(BASE_URL, json=data, headers={'Content-Type': 'application/json'})
        pretty_print_response(response)
        
        if response.status_code == 201:
            print("✅ Medicine created successfully!")
        else:
            print("❌ Failed to create medicine")
            
    except requests.RequestException as e:
        print(f"❌ Request failed: {e}")

def get_all_medicines():
    """Get all medicines"""
    print("📋 Getting all medicines...")
    
    try:
        response = requests.get(BASE_URL)
        pretty_print_response(response)
        
        if response.status_code == 200:
            medicines = response.json()
            print(f"✅ Found {len(medicines)} medicine(s)")
        else:
            print("❌ Failed to get medicines")
            
    except requests.RequestException as e:
        print(f"❌ Request failed: {e}")

def get_medicine_by_id(medicine_id):
    """Get medicine by ID"""
    print(f"🔍 Getting medicine with ID: {medicine_id}...")
    
    try:
        response = requests.get(f"{BASE_URL}/{medicine_id}")
        pretty_print_response(response)
        
        if response.status_code == 200:
            print("✅ Medicine found!")
        elif response.status_code == 404:
            print("❌ Medicine not found")
        else:
            print("❌ Failed to get medicine")
            
    except requests.RequestException as e:
        print(f"❌ Request failed: {e}")

def update_medicine(medicine_id, json_file="medicine.json"):
    """Update medicine by ID from JSON file"""
    print(f"✏️ Updating medicine {medicine_id} from {json_file}...")
    
    data = load_json(json_file)
    if not data:
        return
    
    try:
        response = requests.put(f"{BASE_URL}/{medicine_id}", json=data, headers={'Content-Type': 'application/json'})
        pretty_print_response(response)
        
        if response.status_code == 200:
            print("✅ Medicine updated successfully!")
        elif response.status_code == 404:
            print("❌ Medicine not found")
        else:
            print("❌ Failed to update medicine")
            
    except requests.RequestException as e:
        print(f"❌ Request failed: {e}")

def delete_medicine(medicine_id):
    """Delete medicine by ID"""
    print(f"🗑️ Deleting medicine with ID: {medicine_id}...")
    
    try:
        response = requests.delete(f"{BASE_URL}/{medicine_id}")
        pretty_print_response(response)
        
        if response.status_code == 204:
            print("✅ Medicine deleted successfully!")
        elif response.status_code == 404:
            print("❌ Medicine not found")
        else:
            print("❌ Failed to delete medicine")
            
    except requests.RequestException as e:
        print(f"❌ Request failed: {e}")

def search_medicines(name):
    """Search medicines by name"""
    print(f"🔎 Searching medicines with name: '{name}'...")
    
    try:
        response = requests.get(f"{BASE_URL}/search", params={'name': name})
        pretty_print_response(response)
        
        if response.status_code == 200:
            medicines = response.json()
            print(f"✅ Found {len(medicines)} medicine(s) matching '{name}'")
        else:
            print("❌ Failed to search medicines")
            
    except requests.RequestException as e:
        print(f"❌ Request failed: {e}")

def create_sample_json():
    """Create sample JSON files"""
    print("📝 Creating sample JSON files...")
    
    # Sample medicine data
    sample_medicine = {
        "name": "Aspirin",
        "description": "Pain relief and anti-inflammatory medication",
        "dosageAmount": 500.0,
        "dosageUnit": "mg"
    }
    
    # Create medicine.json
    with open('medicine.json', 'w') as f:
        json.dump(sample_medicine, f, indent=2)
    
    # Create more sample files
    samples = [
        {
            "name": "Ibuprofen", 
            "description": "Anti-inflammatory pain reliever",
            "dosageAmount": 200.0,
            "dosageUnit": "mg"
        },
        {
            "name": "Vitamin D3",
            "description": "Daily vitamin supplement",
            "dosageAmount": 1000.0,
            "dosageUnit": "IU"
        },
        {
            "name": "Metformin",
            "description": "Diabetes medication",
            "dosageAmount": 500.0,
            "dosageUnit": "mg"
        }
    ]
    
    for i, sample in enumerate(samples, 1):
        filename = f"medicine_{i}.json"
        with open(filename, 'w') as f:
            json.dump(sample, f, indent=2)
        print(f"Created: {filename}")
    
    print("✅ Sample JSON files created!")
    print("Edit any JSON file and run the script to test your API!")

def main():
    parser = argparse.ArgumentParser(description='MediTracker API Testing Script')
    parser.add_argument('command', choices=['create', 'list', 'get', 'update', 'delete', 'search', 'sample'], 
                       help='Command to execute')
    parser.add_argument('--id', type=int, help='Medicine ID (for get, update, delete)')
    parser.add_argument('--file', default='medicine.json', help='JSON file to use (default: medicine.json)')
    parser.add_argument('--name', help='Name to search for')
    
    args = parser.parse_args()
    
    print("🚀 MediTracker API Tester")
    print("=" * 40)
    
    if args.command == 'create':
        create_medicine(args.file)
    elif args.command == 'list':
        get_all_medicines()
    elif args.command == 'get':
        if not args.id:
            print("❌ --id required for get command")
            return
        get_medicine_by_id(args.id)
    elif args.command == 'update':
        if not args.id:
            print("❌ --id required for update command")
            return
        update_medicine(args.id, args.file)
    elif args.command == 'delete':
        if not args.id:
            print("❌ --id required for delete command")
            return
        delete_medicine(args.id)
    elif args.command == 'search':
        if not args.name:
            print("❌ --name required for search command")
            return
        search_medicines(args.name)
    elif args.command == 'sample':
        create_sample_json()

if __name__ == "__main__":
    main()
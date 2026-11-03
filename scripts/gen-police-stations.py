#!/usr/bin/env python3
"""Regenerate the bundled Australian policing facilities dataset.

Downloads every facility point from Geoscience Australia's
Emergency_Management_Facilities/MapServer/2 (POLICING_FACILITY, CC BY 4.0)
and writes app/src/main/assets/police_stations.json.
"""
import datetime
import json
import urllib.request
import urllib.parse

BASE = ("https://services.ga.gov.au/gis/rest/services/"
        "Emergency_Management_Facilities/MapServer/2/query")
OUT = "app/src/main/assets/police_stations.json"


def query(params):
    params["f"] = "json"
    url = BASE + "?" + urllib.parse.urlencode(params)
    with urllib.request.urlopen(url, timeout=60) as r:
        return json.load(r)


stations, offset = [], 0
while True:
    data = query({
        "where": "1=1",
        "outFields": "objectid,facility_name,facility_operationalstatus",
        "returnGeometry": "true",
        "resultOffset": offset,
    })
    features = data.get("features", [])
    if not features:
        break
    for f in features:
        attrs = f["attributes"]
        stations.append({
            "id": attrs["objectid"],
            "n": attrs.get("facility_name"),
            "s": attrs.get("facility_operationalstatus"),
            "c": [f["geometry"]["x"], f["geometry"]["y"]],  # [lon, lat] GDA94
        })
    if data.get("exceededTransferLimit"):
        offset += len(features)
    else:
        break

with open(OUT, "w") as fh:
    json.dump({"updated": datetime.date.today().isoformat(), "stations": stations}, fh,
              separators=(",", ":"))
print(f"wrote {len(stations)} stations to {OUT}")
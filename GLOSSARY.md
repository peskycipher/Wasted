# Glossary

## Triggers

**Trigger**:
A configured condition that, when it occurs, causes Wasted to fire. Each trigger can be enabled independently. _Avoid_: alarm, switch, detector

**Fire**:
To execute the configured destructive action for a trigger: lock now, and wipe data (and/or eSIM, send a Recast) according to settings. One trigger firing never fires another.

**Wipe**:
A device factory reset requested through device admin, optionally including the embedded SIM profiles.

**Recast**:
An alternative wipe action: send a broadcast message to a receiver configured by the user.

## Police-station trigger

**Police-station fence**:
A 50 m radius circle centred on a policing facility point from the Australian police facilities dataset. A fix is inside when it is within the radius. _Avoid_: geofence region, perimeter

**Dwell**:
Continuous time during which every accepted fix is inside the same fence. Dwell ends when an accepted fix lands outside every fence; it is never accumulated across separate fence visits.

**Fix**:
One device location sample. Fixes with accuracy worse than 25 m are discarded: they change neither dwell state nor the dwell timer.

**Station dataset**:
The build-time bundled snapshot of all Australian policing facilities (Geoscience Australia, CC BY 4.0). Only operational facilities are fenced.


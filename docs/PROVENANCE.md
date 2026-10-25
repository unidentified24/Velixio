# Input provenance
The owner supplied two APKs renamed to ZIP and authorized a combined application preserving the existing backend.
Product: Veyra. Existing GitHub repository spelling: Verya.

Original inputs:
- Movie App.zip — SHA-256: 42f22105957878118260cf6401df5cdaf7306bd643d1945e9058e62c8052e32e
- movie App v2.zip — SHA-256: 7e3b14fb78d90c2ec57623d73e712b74f532c5f1923d81049bfedbac011f5917

Both verify with distinct APK v2 signing certificates. They have different Android package identifiers.
Analysis found shared code lineage, not cryptographic proof of common publisher identity.
The owner identifies both as their apps.

This repository contains newly written starter code and analysis documentation.
Original APKs, disassembly archives, embedded SDK credentials and native binaries remain local reference material.
The new package app.veyra is provisional; its ability to authenticate against old servers remains to be verified.

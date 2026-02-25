#!/usr/bin/env python3
# This file is part of CPAchecker,
# a tool for configurable software verification:
# https://cpachecker.sosy-lab.org
#
# SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0

"""
Automated migration script for SV-COMP configurations.

This script automates the process of:
1. Creating new svcompYY configs from svcompXX configs
2. Moving svcompXX configs to unmaintained/ while preserving directory structure
3. Updating paths in moved configs to account for new location
"""

import os
import re
import shutil
import argparse
from pathlib import Path
from typing import List, Tuple, Set


def find_svcomp_files(config_dir: Path, version: int) -> List[Path]:
    """Find all files with svcompXX in their name."""
    pattern = f"svcomp{version}"
    files = []
    
    for root, dirs, filenames in os.walk(config_dir):
        # Skip unmaintained directory
        if 'unmaintained' in Path(root).parts:
            continue
            
        for filename in filenames:
            if pattern in filename:
                files.append(Path(root) / filename)
    
    return sorted(files)


def get_relative_path_from_config(file_path: Path, config_dir: Path) -> Path:
    """Get the relative path of a file from the config directory."""
    return file_path.relative_to(config_dir)


def create_new_version_file(old_file: Path, old_version: int, new_version: int) -> Path:
    """Create a new version of the config file with updated references."""
    # Create new filename
    new_filename = old_file.name.replace(f"svcomp{old_version}", f"svcomp{new_version}")
    new_file = old_file.parent / new_filename
    
    # Read old content
    with open(old_file, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Replace version references
    old_year = str(old_version)
    new_year = str(new_version)
    
    # Replace svcompXX with svcompYY
    content = content.replace(f"svcomp{old_version}", f"svcomp{new_version}")
    
    # Replace SV-COMP'XX with SV-COMP'YY
    content = content.replace(f"SV-COMP'{old_year}", f"SV-COMP'{new_year}")
    content = content.replace(f"SV-COMP{old_year}", f"SV-COMP{new_year}")
    
    # Write new file
    with open(new_file, 'w', encoding='utf-8') as f:
        f.write(content)
    
    return new_file


def move_to_unmaintained(file_path: Path, config_dir: Path, unmaintained_dir: Path) -> Path:
    """Move a file to unmaintained while preserving directory structure."""
    # Get relative path from config dir
    rel_path = file_path.relative_to(config_dir)
    
    # Create target path in unmaintained
    target_path = unmaintained_dir / rel_path
    
    # Create parent directories if needed
    target_path.parent.mkdir(parents=True, exist_ok=True)
    
    # Move the file
    shutil.move(str(file_path), str(target_path))
    
    return target_path


def is_svcomp_version_path(path_str: str, old_version: int) -> bool:
    """Check if a path references a svcompXX config."""
    return f"svcomp{old_version}" in path_str


def calculate_new_relative_path(
    old_file_location: Path,
    new_file_location: Path,
    old_relative_path: str,
    config_dir: Path
) -> str:
    """
    Calculate the new relative path from new_file_location to the target.
    
    Args:
        old_file_location: Original location of the config file
        new_file_location: New location of the config file (in unmaintained)
        old_relative_path: The relative path as it was in the old file
        config_dir: The config directory root
    
    Returns:
        The new relative path from new_file_location to the target
    """
    # Resolve the target file path from the original file location
    old_dir = old_file_location.parent
    target_path = (old_dir / old_relative_path).resolve()
    
    # Calculate relative path from new location to target
    new_dir = new_file_location.parent
    try:
        new_relative_path = os.path.relpath(target_path, new_dir)
        return new_relative_path
    except ValueError:
        # If relative path calculation fails, fall back to old behavior
        return old_relative_path


def fix_paths_in_unmaintained_file(
    file_path: Path,
    original_path: Path,
    config_dir: Path, 
    old_version: int
) -> None:
    """
    Update paths in a file moved to unmaintained to account for new location.
    Only updates paths to maintained configs (not svcompXX configs).
    
    Args:
        file_path: Current path of the file (in unmaintained)
        original_path: Original path of the file (before moving)
        config_dir: The config directory root
        old_version: The old svcomp version number
    """
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_content = content
    lines = content.split('\n')
    updated_lines = []
    
    i = 0
    while i < len(lines):
        line = lines[i]
        updated_line = line
        
        # Skip comment lines but NOT #include directives
        if line.strip().startswith('#') and not line.strip().startswith('#include'):
            updated_lines.append(line)
            i += 1
            continue
        
        # Check if this is a multi-line parallelAlgorithm or restartAlgorithm config
        if ('parallelAlgorithm.configFiles' in line or 'restartAlgorithm.configFiles' in line) and '\\' in line:
            # Gather all continuation lines
            multiline_content = [line]
            j = i + 1
            while j < len(lines) and i < len(lines) - 1 and lines[j-1].rstrip().endswith('\\'):
                multiline_content.append(lines[j])
                j += 1
            
            # Process the multi-line configuration
            processed_lines = process_multiline_config(multiline_content, original_path, file_path, config_dir, old_version)
            updated_lines.extend(processed_lines)
            i = j
            continue
        
        # Match various path patterns in config files
        # This handles: property = path, #include path, config = path, etc.
        path_patterns = [
            (r'(#include\s+)([^\s]+)', 2),  # #include path
            (r'([\w\.]+\.config\s*=\s*)([^\s]+)', 2),  # property.config = path (including multi-level like counterexample.checker.config)
            (r'(specification\s*=\s*)([^\s]+)', 2),  # specification = path
        ]
        
        for pattern, group_idx in path_patterns:
            match = re.search(pattern, updated_line)
            if match:
                path_value = match.group(group_idx)
                
                # Skip if it's a svcompXX reference (these stay the same)
                if is_svcomp_version_path(path_value, old_version):
                    continue
                
                # Skip absolute paths or special values
                if path_value.startswith('/') or '=' in path_value or not ('/' in path_value or '.properties' in path_value or '.spc' in path_value):
                    continue
                
                # Calculate the proper new relative path
                new_path = calculate_new_relative_path(
                    original_path,
                    file_path,
                    path_value,
                    config_dir
                )
                
                updated_line = updated_line.replace(path_value, new_path)
        
        updated_lines.append(updated_line)
        i += 1
    
    new_content = '\n'.join(updated_lines)
    
    # Only write if content changed
    if new_content != original_content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(new_content)


def process_multiline_config(
    lines: List[str],
    original_path: Path,
    file_path: Path,
    config_dir: Path,
    old_version: int
) -> List[str]:
    """
    Process a multi-line configuration like parallelAlgorithm.configFiles.
    Simply updates any paths found in each line.
    
    Args:
        lines: List of lines making up the multi-line config (including continuations)
        original_path: Original location of the config file
        file_path: Current location of the config file
        config_dir: The config directory root
        old_version: The old svcomp version number
    
    Returns:
        List of updated lines
    """
    result_lines = []
    
    for line in lines:
        updated_line = line
        
        # Find all potential paths in the line (things ending with .properties or .spc)
        # Pattern: optional path components, filename, optional ::condition
        path_pattern = r'(\S+\.(?:properties|spc))(\s*::[^\s,\\]+)?'
        
        def replace_path(match):
            full_match = match.group(0)
            path_part = match.group(1)
            condition_part = match.group(2) if match.group(2) else ''
            
            # Skip if it's a svcompXX reference
            if is_svcomp_version_path(path_part, old_version):
                return full_match
            
            # Calculate the new relative path
            try:
                new_path = calculate_new_relative_path(
                    original_path,
                    file_path,
                    path_part,
                    config_dir
                )
                return new_path + condition_part
            except:
                # If path calculation fails, return original
                return full_match
        
        updated_line = re.sub(path_pattern, replace_path, updated_line)
        result_lines.append(updated_line)
    
    return result_lines


def update_svcomp_references_in_maintained_files(
    config_dir: Path,
    old_version: int,
    new_version: int
) -> List[Path]:
    """
    Update references to svcompXX configs in maintained files to point to svcompYY.
    This only updates files that are NOT svcomp configs themselves and NOT in unmaintained.
    """
    updated_files = []
    unmaintained_dir = config_dir / 'unmaintained'
    
    # Find all .properties files in maintained area
    for root, dirs, filenames in os.walk(config_dir):
        # Skip unmaintained directory
        if 'unmaintained' in Path(root).parts:
            continue
        
        for filename in filenames:
            if not filename.endswith('.properties'):
                continue
            
            file_path = Path(root) / filename
            
            # Skip svcomp version files (both old and new)
            if f"svcomp{old_version}" in filename or f"svcomp{new_version}" in filename:
                continue
            
            # Read file and check if it contains references to svcompXX
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # Check if file contains any svcompXX references
            if f"svcomp{old_version}" not in content:
                continue
            
            # Update the content
            original_content = content
            
            # Replace svcompXX with svcompYY
            content = content.replace(f"svcomp{old_version}", f"svcomp{new_version}")
            
            # Replace SV-COMP'XX with SV-COMP'YY (in comments/documentation)
            old_year = str(old_version)
            new_year = str(new_version)
            content = content.replace(f"SV-COMP'{old_year}", f"SV-COMP'{new_year}")
            content = content.replace(f"SV-COMP{old_year}", f"SV-COMP{new_year}")
            
            # Only write if content changed
            if content != original_content:
                with open(file_path, 'w', encoding='utf-8') as f:
                    f.write(content)
                updated_files.append(file_path)
    
    return updated_files


def migrate_svcomp_configs(config_dir: Path, old_version: int, new_version: int, dry_run: bool = False):
    """Main migration function."""
    config_dir = config_dir.resolve()
    unmaintained_dir = config_dir / 'unmaintained'
    
    print(f"Migrating SV-COMP configs from {old_version} to {new_version}")
    print(f"Config directory: {config_dir}")
    print(f"Unmaintained directory: {unmaintained_dir}")
    
    if dry_run:
        print("\n*** DRY RUN MODE - No files will be modified ***\n")
    
    # Find all svcompXX files
    old_files = find_svcomp_files(config_dir, old_version)
    
    if not old_files:
        print(f"No svcomp{old_version} files found!")
        return
    
    print(f"\nFound {len(old_files)} svcomp{old_version} files:")
    for f in old_files:
        print(f"  - {f.relative_to(config_dir)}")
    
    if dry_run:
        print("\nDry run - would process the files above")
        return
    
    # Track created and moved files
    created_files = []
    moved_files = []
    
    # Step 1: Create new version files
    print(f"\n=== Step 1: Creating svcomp{new_version} files ===")
    for old_file in old_files:
        rel_path = old_file.relative_to(config_dir)
        print(f"Creating new version of: {rel_path}")
        new_file = create_new_version_file(old_file, old_version, new_version)
        created_files.append(new_file)
        print(f"  -> Created: {new_file.relative_to(config_dir)}")
    
    # Step 2: Move old version files to unmaintained
    print(f"\n=== Step 2: Moving svcomp{old_version} files to unmaintained ===")
    for old_file in old_files:
        rel_path = old_file.relative_to(config_dir)
        print(f"Moving: {rel_path}")
        
        # Store original path before moving
        original_path = old_file
        
        # Move file
        new_location = move_to_unmaintained(old_file, config_dir, unmaintained_dir)
        moved_files.append((new_location, original_path))
        print(f"  -> Moved to: {new_location.relative_to(config_dir)}")
    
    # Step 3: Fix paths in moved files
    print(f"\n=== Step 3: Fixing paths in moved svcomp{old_version} files ===")
    for moved_file, original_path in moved_files:
        rel_path = moved_file.relative_to(config_dir)
        print(f"Fixing paths in: {rel_path}")
        fix_paths_in_unmaintained_file(moved_file, original_path, config_dir, old_version)
        print(f"  -> Updated relative paths")
    
    # Step 4: Update references in maintained files from svcompXX to svcompYY
    print(f"\n=== Step 4: Updating svcomp{old_version} references to svcomp{new_version} in maintained files ===")
    maintained_updated_files = update_svcomp_references_in_maintained_files(config_dir, old_version, new_version)
    if maintained_updated_files:
        for updated_file in maintained_updated_files:
            print(f"  -> Updated: {updated_file.relative_to(config_dir)}")
    else:
        print("  -> No maintained files needed updating")
    
    # Summary
    print("\n=== Migration Complete ===")
    print(f"Created {len(created_files)} new svcomp{new_version} files")
    print(f"Moved {len(moved_files)} svcomp{old_version} files to unmaintained")
    print(f"Updated paths in {len(moved_files)} moved files")
    print(f"Updated svcomp{old_version} references in {len(maintained_updated_files)} maintained files")
    print("\nNext steps:")
    print("1. Review the changes with: git diff")
    print("2. Test the old, new, and unmaintained configs on SV-COMP benchmarks")
    print("3. Update test/test-sets benchmark definitions")
    print("4. Update scripts/smoketest.sh")


def main():
    parser = argparse.ArgumentParser(
        description='Migrate SV-COMP configuration files from one version to another',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Example usage:
  # Migrate from svcomp26 to svcomp27
  python migrate_svcomp_configs.py 26 27
  
  # Dry run to see what would happen
  python migrate_svcomp_configs.py 26 27 --dry-run
  
  # Specify custom config directory
  python migrate_svcomp_configs.py 26 27 --config-dir /path/to/config
        """
    )
    
    parser.add_argument(
        'old_version',
        type=int,
        help='Old SV-COMP version number (e.g., 26 for svcomp26)'
    )
    
    parser.add_argument(
        'new_version',
        type=int,
        help='New SV-COMP version number (e.g., 27 for svcomp27)'
    )
    
    parser.add_argument(
        '--config-dir',
        type=Path,
        default=Path(__file__).parent,
        help='Path to config directory (default: directory containing this script)'
    )
    
    parser.add_argument(
        '--dry-run',
        action='store_true',
        help='Show what would be done without making any changes'
    )
    
    args = parser.parse_args()
    
    if args.new_version <= args.old_version:
        parser.error(f"New version ({args.new_version}) must be greater than old version ({args.old_version})")
    
    if not args.config_dir.exists():
        parser.error(f"Config directory does not exist: {args.config_dir}")
    
    migrate_svcomp_configs(args.config_dir, args.old_version, args.new_version, args.dry_run)


if __name__ == '__main__':
    main()

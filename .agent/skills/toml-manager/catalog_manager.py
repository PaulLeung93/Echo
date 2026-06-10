import sys
import argparse
from tomlkit import parse, dumps, table

# Path to your version catalog (adjust if yours is in a different spot)
CATALOG_PATH = "gradle/libs.versions.toml"

def load_catalog():
    try:
        with open(CATALOG_PATH, "r") as f:
            return parse(f.read())
    except FileNotFoundError:
        print(f"Error: Could not find {CATALOG_PATH}")
        sys.exit(1)

def save_catalog(data):
    with open(CATALOG_PATH, "w") as f:
        f.write(dumps(data))
    print(f"Successfully updated {CATALOG_PATH}")

def add_version(key, value):
    data = load_catalog()
    if "versions" not in data:
        data.add("versions", table())
    
    if key in data["versions"]:
        print(f"Version '{key}' already exists. Updating...")
    
    data["versions"][key] = value
    save_catalog(data)

def add_library(alias, group, name, version_ref):
    data = load_catalog()
    if "libraries" not in data:
        data.add("libraries", table())

    # Construct the library entry: { group = "...", name = "...", version.ref = "..." }
    lib_entry = {"group": group, "name": name, "version": {"ref": version_ref}}
    
    # Normalize alias (replace - with _ or . based on preference, standard is - to -)
    # But TOML keys usually prefer hyphens or underscores.
    
    data["libraries"][alias] = lib_entry
    save_catalog(data)

def main():
    parser = argparse.ArgumentParser(description="Manage libs.versions.toml")
    subparsers = parser.add_subparsers(dest="command")

    # Command: add-version name "1.0.0"
    v_parser = subparsers.add_parser("add-version")
    v_parser.add_argument("name", help="Name of the version variable")
    v_parser.add_argument("value", help="The version string")

    # Command: add-lib alias group name version_ref
    l_parser = subparsers.add_parser("add-lib")
    l_parser.add_argument("alias", help="Library alias (e.g. retrofit-core)")
    l_parser.add_argument("group", help="Group ID")
    l_parser.add_argument("name", help="Artifact ID")
    l_parser.add_argument("version_ref", help="Reference to variable in [versions]")

    args = parser.parse_args()

    if args.command == "add-version":
        add_version(args.name, args.value)
    elif args.command == "add-lib":
        add_library(args.alias, args.group, args.name, args.version_ref)
    else:
        parser.print_help()

if __name__ == "__main__":
    main()
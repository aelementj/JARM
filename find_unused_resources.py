import os
import re
from xml.etree import ElementTree

# --- Configuration ---
RESOURCE_FILES = {
    "color": "app/src/main/res/values/colors.xml",
    "dimen": "app/src/main/res/values/dimens.xml",
    "string": "app/src/main/res/values/strings.xml",
    "style": "app/src/main/res/values/styles.xml",
    "theme": "app/src/main/res/values/themes.xml", # Themes are a type of style
}

SEARCH_DIRECTORIES = [
    "app/src/main/res/layout",
    "app/src/main/res/menu",
    "app/src/main/res/drawable",
    "app/src/main/res/color",
    "app/src/main/java",
]
MANIFEST_FILE = "app/src/main/AndroidManifest.xml"
OUTPUT_FILE = "unused_resources_report.txt"

# --- Resource Parsing Functions ---

def parse_xml_resources(filepath, resource_tag, name_attribute="name"):
    """Parses an XML file and extracts resource names for a given tag."""
    defined_resources = set()
    if not os.path.exists(filepath):
        print(f"Warning: Resource file not found: {filepath}")
        return defined_resources
    try:
        tree = ElementTree.parse(filepath)
        root = tree.getroot()
        for element in root.findall(f".//{resource_tag}"):
            name = element.get(name_attribute)
            if name:
                defined_resources.add(name)
    except ElementTree.ParseError as e:
        print(f"Error parsing XML file {filepath}: {e}")
    return defined_resources

def get_defined_resources():
    """Gets all defined resources from the configured files."""
    defined = {
        "color": set(),
        "string": set(),
        "dimen": set(),
        "style": set(), # Includes styles and themes
    }
    if os.path.exists(RESOURCE_FILES["color"]):
        defined["color"].update(parse_xml_resources(RESOURCE_FILES["color"], "color"))
    if os.path.exists(RESOURCE_FILES["dimen"]):
        defined["dimen"].update(parse_xml_resources(RESOURCE_FILES["dimen"], "dimen"))
    if os.path.exists(RESOURCE_FILES["string"]):
        defined["string"].update(parse_xml_resources(RESOURCE_FILES["string"], "string"))

    # Styles and Themes
    if os.path.exists(RESOURCE_FILES["style"]):
        defined["style"].update(parse_xml_resources(RESOURCE_FILES["style"], "style"))
    if os.path.exists(RESOURCE_FILES["theme"]): # themes.xml also contains 'style' tags
        defined["style"].update(parse_xml_resources(RESOURCE_FILES["theme"], "style"))

    return defined

# --- Usage Search Functions ---

def get_files_to_search():
    """Gathers all files to search for resource usages."""
    files_to_search = []
    if os.path.exists(MANIFEST_FILE):
        files_to_search.append(MANIFEST_FILE)
    else:
        print(f"Warning: Manifest file not found: {MANIFEST_FILE}")

    for directory in SEARCH_DIRECTORIES:
        if not os.path.exists(directory):
            print(f"Warning: Search directory not found: {directory}")
            continue
        for root, _, files in os.walk(directory):
            for file in files:
                if file.endswith((".xml", ".java")):
                    files_to_search.append(os.path.join(root, file))
    return files_to_search

def find_used_resources(files_to_search, defined_resources):
    """Searches for resource usages in the given files."""
    used = {
        "color": set(),
        "string": set(),
        "dimen": set(),
        "style": set(),
    }

    # Precompile regex patterns for efficiency
    patterns = {
        "color": re.compile(r"(@color/|R\.color\.)([a-zA-Z0-9_]+)"),
        "string": re.compile(r"(@string/|R\.string\.)([a-zA-Z0-9_]+)"),
        "dimen": re.compile(r"(@dimen/|R\.dimen\.)([a-zA-Z0-9_]+)"),
        # Style usage: style="@style/MyTheme", android:theme="@style/MyTheme", parent="MyTheme", R.style.MyTheme
        "style": re.compile(r"(@style/|R\.style\.|parent=\")([a-zA-Z0-9_.]+)")
    }

    for filepath in files_to_search:
        try:
            with open(filepath, "r", encoding="utf-8", errors="ignore") as f:
                content = f.read()
                for res_type, pattern in patterns.items():
                    for match in pattern.finditer(content):
                        resource_name = match.group(2)
                        # Styles can have parent definitions like "Theme.AppCompat.Light"
                        # We are only interested in the final part of such names if defined locally
                        if '.' in resource_name and res_type == "style":
                            resource_name_parts = resource_name.split('.')
                            # Check if any part of the qualified name is a defined style
                            for part in resource_name_parts:
                                if part in defined_resources["style"]:
                                    used["style"].add(part)
                                    break # Found a match, no need to check other parts
                            # Also add the full name if it's defined (e.g. "Theme.App")
                            if resource_name in defined_resources["style"]:
                                 used["style"].add(resource_name)
                        elif resource_name in defined_resources[res_type]:
                             used[res_type].add(resource_name)
        except Exception as e:
            print(f"Error reading or processing file {filepath}: {e}")
    return used

# --- Main Execution ---
def main():
    print("Starting unused resource scan...")
    defined_resources = get_defined_resources()

    print("\nDefined resources:")
    for res_type, names in defined_resources.items():
        print(f"  {res_type.capitalize()}: {len(names)}")

    files_to_search = get_files_to_search()
    print(f"\nSearching {len(files_to_search)} files for usages...")

    used_resources = find_used_resources(files_to_search, defined_resources)

    print("\nUsed resources (found in code/layouts):")
    for res_type, names in used_resources.items():
        print(f"  {res_type.capitalize()}: {len(names)}")

    unused_resources = {
        "color": defined_resources["color"] - used_resources["color"],
        "string": defined_resources["string"] - used_resources["string"],
        "dimen": defined_resources["dimen"] - used_resources["dimen"],
        "style": defined_resources["style"] - used_resources["style"],
    }

    print("\nUnused resources:")
    with open(OUTPUT_FILE, "w") as f:
        total_unused = 0
        for res_type, names in unused_resources.items():
            count = len(names)
            total_unused += count
            print(f"  Unused {res_type.capitalize()}: {count}")
            if names:
                f.write(f"Unused {res_type.capitalize()}:\n")
                for name in sorted(list(names)):
                    f.write(f"  - {name}\n")
                f.write("\n")
        if total_unused == 0:
            f.write("No unused resources found.\n")

    print(f"\nReport written to {OUTPUT_FILE}")
    print("Scan finished.")

if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""
Terraform MCP Validation Script

This script connects to HashiCorp's Terraform MCP Server and validates
Terraform plans against the official Terraform Registry.
"""

import json
import subprocess
import sys
import os
from typing import Dict, List, Optional, Any

# MCP Server Docker image
MCP_SERVER_IMAGE = "hashicorp/terraform-mcp-server:latest"


def send_mcp_request(method: str, params: Dict[str, Any] = None) -> Dict[str, Any]:
    """Send a JSON-RPC request to the MCP server via Docker stdio."""
    request = {
        "jsonrpc": "2.0",
        "id": 1,
        "method": method
    }
    
    if params:
        request["params"] = params
    
    try:
        process = subprocess.Popen(
            ["docker", "run", "--rm", "-i", MCP_SERVER_IMAGE],
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True
        )
        
        request_json = json.dumps(request) + "\n"
        stdout, stderr = process.communicate(input=request_json, timeout=30)
        
        if process.returncode != 0:
            print(f"Error: MCP server returned non-zero exit code: {stderr}", file=sys.stderr)
            return {}
        
        try:
            response = json.loads(stdout.strip())
            return response.get("result", {})
        except json.JSONDecodeError as e:
            print(f"Error parsing MCP response: {e}", file=sys.stderr)
            print(f"Response: {stdout}", file=sys.stderr)
            return {}
            
    except subprocess.TimeoutExpired:
        print("Error: MCP server request timed out", file=sys.stderr)
        return {}
    except Exception as e:
        print(f"Error communicating with MCP server: {e}", file=sys.stderr)
        return {}


def initialize_mcp() -> bool:
    """Initialize the MCP server connection."""
    params = {
        "protocolVersion": "2024-11-05",
        "clientInfo": {
            "name": "terraform-mcp-validator",
            "version": "1.0.0"
        }
    }
    
    result = send_mcp_request("initialize", params)
    return "capabilities" in result


def get_provider_version(namespace: str, provider: str) -> Optional[str]:
    """Get the latest version of a provider from the Terraform Registry."""
    params = {
        "namespace": namespace,
        "name": provider
    }
    
    result = send_mcp_request("get_latest_provider_version", params)
    return result.get("version") if result else None


def search_modules(provider: str, limit: int = 5) -> List[Dict[str, Any]]:
    """Search for modules related to a provider."""
    params = {
        "query": provider,
        "limit": limit
    }
    
    result = send_mcp_request("search_modules", params)
    return result.get("modules", []) if result else []


def get_resource_docs(namespace: str, provider: str, resource: str) -> Optional[Dict[str, Any]]:
    """Get documentation for a specific resource."""
    params = {
        "namespace": namespace,
        "name": provider,
        "resource": resource
    }
    
    result = send_mcp_request("get_resource_docs", params)
    return result if result else None


def extract_providers_from_plan(plan_path: str) -> List[Dict[str, str]]:
    """Extract unique providers from a Terraform plan JSON file."""
    try:
        with open(plan_path, 'r') as f:
            plan = json.load(f)
        
        providers = set()
        provider_list = []
        
        # Extract providers from configuration
        config = plan.get("configuration", {})
        provider_configs = config.get("provider_configs", {})
        
        for provider_key, provider_data in provider_configs.items():
            # Provider key format: "aws", "azurerm", etc.
            if provider_key not in providers:
                providers.add(provider_key)
                # Try to determine namespace (usually "hashicorp" for official providers)
                namespace = "hashicorp"
                provider_list.append({
                    "namespace": namespace,
                    "name": provider_key
                })
        
        return provider_list
        
    except FileNotFoundError:
        print(f"Warning: Plan file not found: {plan_path}", file=sys.stderr)
        return []
    except json.JSONDecodeError as e:
        print(f"Error parsing plan file: {e}", file=sys.stderr)
        return []
    except Exception as e:
        print(f"Error extracting providers: {e}", file=sys.stderr)
        return []


def analyze_plan_resources(plan_path: str) -> Dict[str, Any]:
    """Analyze resources in the Terraform plan."""
    try:
        with open(plan_path, 'r') as f:
            plan = json.load(f)
        
        resource_changes = plan.get("resource_changes", [])
        
        actions = {
            "create": 0,
            "update": 0,
            "delete": 0,
            "replace": 0
        }
        
        resources = []
        
        for change in resource_changes:
            actions_list = change.get("change", {}).get("actions", [])
            for action in actions_list:
                if action in actions:
                    actions[action] += 1
            
            resource_type = change.get("type", "unknown")
            resource_name = change.get("name", "unknown")
            resources.append({
                "type": resource_type,
                "name": resource_name,
                "actions": actions_list
            })
        
        return {
            "actions": actions,
            "resources": resources,
            "total_resources": len(resources)
        }
        
    except Exception as e:
        print(f"Error analyzing plan: {e}", file=sys.stderr)
        return {
            "actions": {},
            "resources": [],
            "total_resources": 0
        }


def main():
    """Main validation function."""
    plan_path = "tfplan.json"
    
    if not os.path.exists(plan_path):
        print(f"Error: Plan file not found: {plan_path}", file=sys.stderr)
        sys.exit(1)
    
    print("Initializing MCP server connection...")
    if not initialize_mcp():
        print("Warning: Failed to initialize MCP server", file=sys.stderr)
        sys.exit(1)
    
    print("Extracting providers from Terraform plan...")
    providers = extract_providers_from_plan(plan_path)
    
    if not providers:
        print("Warning: No providers found in plan", file=sys.stderr)
        providers = [{"namespace": "hashicorp", "name": "aws"}]  # Default for testing
    
    print("Analyzing Terraform plan...")
    plan_analysis = analyze_plan_resources(plan_path)
    
    # Generate validation report
    report_lines = [
        "# Terraform MCP Validation Report\n",
        f"## Plan Summary\n",
        f"- Total Resources: {plan_analysis['total_resources']}\n",
        f"- Actions:\n",
        f"  - Create: {plan_analysis['actions'].get('create', 0)}\n",
        f"  - Update: {plan_analysis['actions'].get('update', 0)}\n",
        f"  - Delete: {plan_analysis['actions'].get('delete', 0)}\n",
        f"  - Replace: {plan_analysis['actions'].get('replace', 0)}\n",
        f"\n## Provider Validation\n"
    ]
    
    for provider in providers:
        namespace = provider["namespace"]
        name = provider["name"]
        
        print(f"Validating provider: {namespace}/{name}")
        
        version = get_provider_version(namespace, name)
        modules = search_modules(name, limit=3)
        
        report_lines.append(f"### {namespace}/{name}\n")
        
        if version:
            report_lines.append(f"- Latest Version: `{version}`\n")
        else:
            report_lines.append(f"- Latest Version: Unable to determine\n")
        
        if modules:
            report_lines.append(f"- Recommended Modules:\n")
            for module in modules[:3]:
                module_name = module.get("name", "unknown")
                report_lines.append(f"  - {module_name}\n")
        else:
            report_lines.append(f"- Recommended Modules: None found\n")
        
        report_lines.append("\n")
    
    # Write report
    report_content = "".join(report_lines)
    with open("mcp_validation_report.txt", "w") as f:
        f.write(report_content)
    
    print("\nValidation complete!")
    print(f"Report written to: mcp_validation_report.txt")
    
    # Create a simple AI analysis placeholder
    ai_analysis = f"""## Terraform Plan Analysis

This plan includes:
- {plan_analysis['total_resources']} resource changes
- Provider validation completed via HashiCorp MCP Server

### Recommendations:
1. Review provider versions to ensure you're using the latest stable versions
2. Consider using recommended modules from the Terraform Registry
3. Verify all resource configurations match the latest provider documentation

*Note: This is a basic analysis. For more detailed insights, integrate with GitHub Models API.*
"""
    
    with open("ai_analysis.txt", "w") as f:
        f.write(ai_analysis)
    
    print("AI analysis written to: ai_analysis.txt")


if __name__ == "__main__":
    main()


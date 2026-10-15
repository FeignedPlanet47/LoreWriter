from .schemas import CollaboratorPermissions

DEFAULTS = {
    "owner": dict(blocks="full", lines="full", script="full", members="full", export="share"),
    "admin": dict(blocks="full", lines="full", script="full", members="manage", export="share"),
    "editor": dict(blocks="edit", lines="edit", script="edit", members="none", export="export"),
    "reader": dict(blocks="view", lines="view", script="read", members="none", export="none"),
}

# Server-side ceilings: custom permissions cannot grant access above the role.
ORDER = {
    "blocks": ["view", "move", "edit", "full"],
    "lines": ["view", "move", "edit", "full"],
    "script": ["read", "edit", "full"],
    "members": ["none", "invite", "manage", "full"],
    "export": ["none", "export", "share"],
}

def effective(role: str, custom: dict | None = None) -> dict:
    defaults = DEFAULTS[role]
    if role == "owner":
        return defaults.copy()
    requested = CollaboratorPermissions.model_validate(custom or defaults).model_dump()
    return {k: ORDER[k][min(ORDER[k].index(v), ORDER[k].index(defaults[k]))] for k, v in requested.items()}

def validate_custom(role: str, custom: dict) -> None:
    if effective(role, custom) != custom:
        raise ValueError("Permissions exceed role ceiling")

{
    "data": {
        "nodes": [
            <#list nodes as node>
                <#switch node.type>
                    <#case "Module">
                        <#assign label = "${node.name}">
                        <#assign color = "navy">
                        <#assign size = "7">
                        <#break>
                    <#case "Service definition">
                        <#assign label = "${node.name}">
                        <#assign color = "brown">
                        <#assign size = "5">
                        <#break>
                    <#case "Service">
                        <#assign label = "${node.name}">
                        <#assign color = "brown">
                        <#assign size = "3">
                        <#break>
                    <#case "Package">
                        <#assign label = "${node.name}">
                        <#assign color = "orange">
                        <#assign size = "3">
                        <#break>
                    </#switch>

                {
                    "ID": "${node.id}",
                    "LABEL": "${label}",
                    "COLORVALUE": "${color}",
                    "COLORLABEL": "${node.type}",
                    "SIZEVALUE": "${size}",
                    "INFOSTRING": "${node.name}"
                }
                <#sep>,</#sep>
            </#list>
        ],
        "links": [
            <#list links as link>
                <#switch link.label>
                    <#case "Exports">
                        <#assign line = "solid">
                        <#assign lineColor = "orange">
                        <#break>
                    <#case "Provides">
                        <#assign line = "solid">
                        <#assign lineColor = "brown">
                        <#break>
                    <#case "Uses">
                        <#assign line = "solid">
                        <#assign lineColor = "blue">
                        <#break>
                    <#default >
                        <#assign line = "solid">
                        <#assign lineColor = "gray">
                        <#break>
                    </#switch>
                {
                    "FROMID": "${link.from}",
                    "TOID": "${link.to}",
                    "STYLE": "${line}",
                    "INFOSTRING": "${link.label}",
                    "COLOR": "${lineColor}"
                }
                <#sep>,</#sep>
            </#list>
        ]
    }
}
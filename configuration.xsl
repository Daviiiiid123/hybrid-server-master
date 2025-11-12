<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:r="http://www.esei.uvigo.es/dai/hybridserver"
    xmlns="http://www.esei.uvigo.es/dai/hybridserver">
    
    <xsl:output method="html" />
    
    <xsl:template match="/">
        <xsl:text disable-output-escaping="yes">&lt;!DOCTYPE html&gt;</xsl:text>
        <html>
            <head>
                <title>Configuracion</title>
            </head>
            <body>
                <div id="container">
                    <h1>Configuracion</h1>
                    
                    <h2>Conexion</h2>
                    <div id="connections">
                        <xsl:apply-templates select="/r:configuration/r:connections"/>
                    </div>
                    <h2>DataBase</h2>
                    <div id="database">
                        <xsl:apply-templates select="/r:configuration/r:database"/>
                    </div>
                    <h2>Servidores</h2>
                    <div id="servers">
                        <xsl:apply-templates select="/r:configuration/r:servers/r:server"/>
                    </div>
                </div>
            </body>
        </html>
    </xsl:template>
    
    
    <xsl:template match="r:server">
        <div>
        <h3><xsl:value-of select="@name"/></h3>
            <div class="info">
                <div class="wsdl" >
                    <strong>wsdl: </strong>&#160;<xsl:value-of select="@wsdl"/>
                </div>
                
                <div class="namespace" >
                    <strong>Namespace: </strong>&#160;<xsl:value-of select="@namespace"/> 
                </div>
                
                <div class="service" >
                    <strong>Servicio: </strong>&#160;<xsl:value-of select="@service"/> 
                </div>
                
                <div class="httpAddress" >
                    <strong>httpAddress </strong>&#160;<xsl:value-of select="@httpAddress"/> 
                </div>
            </div>	
		</div>
    </xsl:template>
    
    <xsl:template match="r:connections">
        <div>
            <div class="http">
                <div class="http" >
                    <strong>http: </strong>&#160;<xsl:value-of select="r:http/."/>
                </div>
                
                <div class="webservice" >
                    <strong>webservice: </strong>&#160;<xsl:value-of select="r:webservice/."/> 
                </div>
                
                <div class="numClients" >
                    <strong>Numero de clientes: </strong>&#160;<xsl:value-of select="r:numClients/."/> 
                </div>
                
            </div>	
		</div>
	</xsl:template>
	
    <xsl:template match="r:database">
        <div>
            <div class="info">
                <div class="user" >
                    <strong>user: </strong>&#160;<xsl:value-of select="r:user"/>
                </div>
                
                <div class="password" >
                    <strong>password: </strong>&#160;<xsl:value-of select="r:password"/> 
                </div>
                
                <div class="url" >
                    <strong>Url: </strong>&#160;<xsl:value-of select="r:url"/> 
                </div>
                
            </div>	
		</div>
    </xsl:template>
    
</xsl:stylesheet>
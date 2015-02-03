<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:template match="/success">
    <html>
      <head>
        <title>Directions from <xsl:value-of select="parameters/start/@value"/> to <xsl:value-of select="parameters/end/@value"/></title>
        <link rel="stylesheet" href="styles.css" type="text/css" media="all" title="Normal" />
      </head>
      <body>
        <img class="header" src="images/header.png" alt="MeeshQuest" width="400" height="60"/>
        <h2>Directions from <xsl:value-of select="parameters/start/@value"/> to <xsl:value-of select="parameters/end/@value"/></h2>
        <div id="directions">
          <p>
          <ol>
            <li>Start at <strong><xsl:value-of select="parameters/start/@value"/></strong></li>
            <xsl:apply-templates select="output/path"/>
            <li>Arrive at <strong><xsl:value-of select="parameters/end/@value"/></strong></li>
          </ol>
          </p>
        </div>
        <img src="{parameters/saveHTML/@value}.png" alt="map"/>
      </body>
    </html>
  </xsl:template>
  
  <xsl:template match="path">
    <xsl:if test="count(*) > 0">
      <li>Get on <strong>(<xsl:value-of select="road/@start"/>, <xsl:value-of select="road/@end"/>)</strong></li>
      <xsl:if test="count(*) > 1">
        <xsl:for-each select="left|right|straight">
          <li>
            <xsl:choose>
              <xsl:when test="name()='straight'">
                Go <strong>STRAIGHT</strong> onto
              </xsl:when>
              <xsl:when test="name()='left'">
                Turn <strong>LEFT</strong> on
              </xsl:when>
              <xsl:otherwise>
                Turn <strong>RIGHT</strong> on
              </xsl:otherwise>
            </xsl:choose>
            <strong>(<xsl:value-of select="following-sibling::*/@start"/>, <xsl:value-of select="following-sibling::*/@end"/>)</strong>
          </li>
        </xsl:for-each>
      </xsl:if>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>
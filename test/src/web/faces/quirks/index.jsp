<%@ page language="java" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://metawidget.org/faces" prefix="m" %>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<c:set var="aNull" value="${null}" scope="request"/>

<html>
	<body>

		<f:view>
		
			<h:form id="form">
				<m:metawidget value="#{quirks}">
					<f:param name="columns" value="2"/>
				</m:metawidget>
			</h:form>
						
			<m:metawidget value="#{quirks2}">
				<f:param name="columns" value="0"/>
				<m:stub value="#{quirks2.refresh}"/>
			</m:metawidget>

			<m:metawidget value="#{quirks2}">
				<f:param name="columns" value="0"/>
				<m:stub value="#{quirks2.refresh}"/>
			</m:metawidget>

			<m:metawidget value="#{aNull}"/>
						
		</f:view>
		
	</body>	    
</html>

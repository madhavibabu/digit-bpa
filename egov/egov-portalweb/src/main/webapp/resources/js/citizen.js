/*
 *    eGov  SmartCity eGovernance suite aims to improve the internal efficiency,transparency,
 *    accountability and the service delivery of the government  organizations.
 *
 *     Copyright (C) 2017  eGovernments Foundation
 *
 *     The updated version of eGov suite of products as by eGovernments Foundation
 *     is available at http://www.egovernments.org
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see http://www.gnu.org/licenses/ or
 *     http://www.gnu.org/licenses/gpl.html .
 *
 *     In addition to the terms of the GPL license to be adhered to in using this
 *     program, the following additional terms are to be complied with:
 *
 *         1) All versions of this program, verbatim or modified must carry this
 *            Legal Notice.
 *            Further, all user interfaces, including but not limited to citizen facing interfaces, 
 *            Urban Local Bodies interfaces, dashboards, mobile applications, of the program and any 
 *            derived works should carry eGovernments Foundation logo on the top right corner.
 *
 *            For the logo, please refer http://egovernments.org/html/logo/egov_logo.png.
 *            For any further queries on attribution, including queries on brand guidelines, 
 *            please contact contact@egovernments.org
 *
 *         2) Any misrepresentation of the origin of the material is prohibited. It
 *            is required that all modified versions of this material be marked in
 *            reasonable ways as different from the original version.
 *
 *         3) This license does not grant any rights to any user of the program
 *            with regards to rights under trademark law for use of the trade names
 *            or trademarks of eGovernments Foundation.
 *
 *   In case of any queries, you can reach eGovernments Foundation at contact@egovernments.org.
 *
 */

$(document).ready(function(){
	
	//Global variable
	
	var clickedServiceData;
	var selectedService;
	var clientId = $('#clientId').val();
	
	//---------------
	
	// AJAX Call -------------------
	$.ajax({
        url: '/portal/rest/fetch/servicespending',
//        dataType: 'text',
        type: 'POST',
        contentType: 'application/x-www-form-urlencoded',
        data: {
        	id: parseFloat($('#userId').val())
        },
        success: function( data, textStatus, jQxhr ){
        	var tableInitData = configureTableData(data, "portalInboxHelper");
        	var table = $("#bpa-home-table").DataTable( {
                data: tableInitData,
                "columns": [
                    { "data": "srNo", "orderable": false },
                    { "data": "ulbName", "orderable": false },
                    { "data": "applicantName", "orderable": false },
                    { "data": "serviceRequestNo", "orderable": false},
                    { "data": "serviceRequestDate" },
                    { "data": "serviceGroup" },
                    { "data": "serviceName" },
                    { "data": "status" },
                    { "data": "pendingAction", "orderable": false},
                ]

            } );
        	$('#bpa-home-table tbody').on('click', 'tr', function (e) {
        	    var data = table.row( this ).data();
        	    e.stopImmediatePropagation();
        	    openPopUp(data.domainUrl+data.link);
        	} );
        	
        	//Initialize data in global variable
        	window.clickedServiceData = cloneDeep(data);
        	window.selectedService = "all";
        	
        },
        error: function( jqXhr, textStatus, errorThrown ){
            console.log( errorThrown );
        }
    });
	
	
	
	//------------------------------
		
	$(".bpa-service-card").click(function(){
		var url = "";
		switch($(this).attr("name")){
			case "totalServicesApplied": 
				url = "/portal/rest/fetch/servicesapplied";
				break;
			case "servicesUnderScrutiny": 
				url = "/portal/rest/fetch/servicespending";
				break;
			case "servicesCompleted": 
				url = "/portal/rest/fetch/servicescompleted";
				break;
			default: 
				url = "/portal/rest/fetch/servicespending";
				break;
		}
		fetchDataAndInitiateTable(url, "portalInboxHelper");
		
	})

    $('#new-pass').popover({trigger: "focus", placement: "bottom"});

	$('.password-error').hide();
    $('.password-error-msg').hide();

	$('.totalServicesAppliedHide').hide();
	$('.totalServicesCompletedHide').hide();
//	$('#servicesCmpletedDiv').attr('style', 'opacity: 0.7;cursor: pointer');
//	$('#totalServicesAppliedDiv').attr('style', 'opacity: 0.7;cursor: pointer');
	$('#totalServicesAppliedDiv').click(function() {
		$('.servicesUnderScrutinyHide').hide();
		$('.totalServicesCompletedHide').hide();
		$('.totalServicesAppliedHide').show();
//		$('#totalServicesAppliedDiv').attr('style', 'opacity: 1;cursor: pointer');
//		$('#servicesUnderScrutinyDiv').attr('style', 'opacity: 0.7;cursor: pointer');
//		$('#servicesCmpletedDiv').attr('style', 'opacity: 0.7;cursor: pointer');
	});
	
	$('#servicesUnderScrutinyDiv').click(function() {
		$('.totalServicesAppliedHide').hide();
		$('.totalServicesCompletedHide').hide();
		$('.servicesUnderScrutinyHide').show();
//		$('#servicesUnderScrutinyDiv').attr('style', 'opacity: 1;cursor: pointer');
//		$('#servicesCmpletedDiv').attr('style', 'opacity: 0.7;cursor: pointer');
//		$('#totalServicesAppliedDiv').attr('style', 'opacity: 0.7;cursor: pointer');
	});
	
	$(".bpa-home-card").click(function(){
		$(".bpa-home-card").removeClass("clicked-card");
		$(this).addClass("clicked-card");
		$(".bpa-home-card .text").removeClass("color-generic-new");
		$(this).find(".text").addClass("color-generic-new");
	})
	
	$('#servicesCmpletedDiv').click(function() {
		$('.totalServicesAppliedHide').hide();
		$('.servicesUnderScrutinyHide').hide();
		$('.totalServicesCompletedHide').show();
//		$('#servicesCmpletedDiv').attr('style', 'opacity: 1;cursor: pointer');
//		$('#totalServicesAppliedDiv').attr('style', 'opacity: 0.7;cursor: pointer');
//		$('#servicesUnderScrutinyDiv').attr('style', 'opacity: 0.7;cursor: pointer');

	});
	
  var module;

  $('.services .content').matchHeight();
  
  leftmenuheight();
  rightcontentheight();

  //alert(matchRuleShort("bird123", "bird*"))

  $('#search').keyup(function(e){
    var rule = '*'+$(this).val()+'*';
    if(e.keyCode == 8){
      $('[data-services="'+module+'"]').show();
    }
    $(".services-item .services:visible").each(function(){
      var testStr = $(this).find('.content').html().toLowerCase();
      console.log(testStr, rule, matchRuleShort(testStr, rule))
      if(matchRuleShort(testStr, rule))
        $(this).show();
      else
        $(this).hide();
    });
  });

  $('.modules-li').click(function(){
    $('#search').val('');
    $('.modules-li').removeClass('active');
    $(this).addClass('active');
    module = $(this).data('module');
    $('.services-item .services').hide();
    $('#showServiceGroup').hide();
    if(module == 'home'){
      $('.inbox-modules').show();
      $('.action-bar').addClass('hide');
      $('#showServiceGroup').show();
      inboxloadmethod();
    }
    else{
    	  $('.inbox-modules').hide();
    	  $('.module-heading').text(module);  
          $('[data-services="'+module+'"]').show();
          $('.action-bar').removeClass('hide');
    }
  })  
	  
  $( window ).resize(function() {
    leftmenuheight();
    rightcontentheight();
  });

  $('table tbody tr').click(function(){
    $('#myModal').modal('show');
  });

    $('#passwordForm').on('submit', function (e) {
        e.preventDefault();
        $.ajax({
            url: '/egi/home/password/update',
            type: 'GET',
            data: {
                'currentPwd': $("#old-pass").val(),
                'newPwd': $("#new-pass").val(),
                'retypeNewPwd': $("#retype-pass").val()
            },
            success: function (data) {
                var msg = "";
                if (data == "SUCCESS") {
                    $("#old-pass").val("");
                    $("#new-pass").val("");
                    $("#retype-pass").val("");
                    $('.change-password').modal('hide');
                    bootbox.alert("Your password has been updated.");
                    $('.pass-cancel').removeAttr('disabled');
                    $('#pass-alert').hide();
                } else if (data == "NEWPWD_UNMATCH") {
                    msg = "New password you have entered does not match with retyped password.";
                    $("#new-pass").val("");
                    $("#retype-pass").val("");
                    $('.change-password').modal('show');
                } else if (data == "CURRPWD_UNMATCH") {
                    msg = "Old password you have entered is incorrect.";
                    $("#old-pass").val("");
                    $('.change-password').modal('show');
                } else if (data == "NEWPWD_INVALID") {
                    msg = $('.password-error-msg').html();
                    $("#new-pass").val("");
                    $("#retype-pass").val("");
                    $('.change-password').modal('show');
                }
                $('#pwd-incorrt-match').removeClass('alert-success');
                $('#pwd-incorrt-match').addClass('alert-danger');
                $('.password-error').html(msg).show();

            },
            error: function () {
                bootbox.alert("Internal server error occurred, please try after sometime.");
            }
        });

    });

    $('.checkpassword').blur(function () {
        if (($('#new-pass').val() != "") && ($('#retype-pass').val() != "")) {
            if ($('#new-pass').val() == $('#retype-pass').val()) {
                $('#pwd-incorrt-match').removeClass('alert-danger');
                $('#pwd-incorrt-match').addClass('alert-success');
                $('.password-error').show();
                $('.password-error').html('Password is matching');
                $('#retype-pass').addClass('error');
            } else if ($('#new-pass').val() !== $('#retype-pass').val()) {
                $('#pwd-incorrt-match').removeClass('alert-success');
                $('#pwd-incorrt-match').addClass('alert-danger');
                $('.password-error').show();
                $('.password-error').html('Password is not matching');
                $('#retype-pass').addClass('error');
            }
        }
    });
  
  $('#serviceGroup').change(function(){
	  window.selectedService = getServiceGroup($(this).val());
	  initiateTable(window.clickedServiceData, window.selectedService, "portalInboxHelper");
//	  var selected = $(this).val();
//	  var total = $( "#totalServicesAppliedSize" ).html().trim();
//	  var length = document.getElementsByClassName($(this).val()).length / 2;
//	  if($(this).val() == "") {
//		  $('.showAll').show();
//		  $( "#totalServicesAppliedSize" ).html($( "#tabelPortal tbody.totalServicesAppliedHide tr.showAll" ).length);
//		  $( "#totalServicesCompletedSize" ).html($( "#tabelPortal tbody.totalServicesCompletedHide tr.showAll" ).length);
//		  $( "#totalServicesPendingSize" ).html($( "#tabelPortal tbody.servicesUnderScrutinyHide tr.showAll" ).length);
//		  var showAllClass ="#tabelPortal tbody.servicesUnderScrutinyHide tr.showAll td:first-child";
//		  generateSno(showAllClass);
//
//	  } else {
//		  $('.showAll').hide();
//		  $('.'+$(this).val()).show();
//		  $( "#totalServicesAppliedSize" ).html($( "#tabelPortal tbody.totalServicesAppliedHide tr."+$(this).val() ).length);
//		  $( "#totalServicesCompletedSize" ).html($( "#tabelPortal tbody.totalServicesCompletedHide tr."+$(this).val() ).length);
//		  $( "#totalServicesPendingSize" ).html($( "#tabelPortal tbody.servicesUnderScrutinyHide tr."+$(this).val() ).length);
//		  
//		  var servicesUnderScrutinyHideClass ="#tabelPortal tbody.servicesUnderScrutinyHide tr."+ selected + " td:first-child";
//		  var totalServicesAppliedHideClass="#tabelPortal tbody.totalServicesAppliedHide tr."+ selected + " td:first-child";
//		  var totalServicesCompletedHideClass="#tabelPortal tbody.totalServicesCompletedHide tr."+ selected + " td:first-child";
//		  generateSno(servicesUnderScrutinyHideClass);
//		  generateSno(totalServicesAppliedHideClass);
//		  generateSno(totalServicesCompletedHideClass);
//	  }
  });
  
  
  

});

function leftmenuheight(){
  //console.log($( window ).height(), $('.modules-ul').height());
  $('.left-menu,.modules-ul').css({
    height:$( window ).height(),
    overflow : 'auto'
  });
}

function rightcontentheight(){
  //console.log($( window ).height(), $('.right-content').height());
  $('.right-content').css({
//    height:$( window ).height(),
//    overflow : 'auto'
  })
}

//Short code
function matchRuleShort(str, rule) {
  return new RegExp("^" + rule.toLowerCase().split("*").join(".*") + "$").test(str.toLowerCase());
}

var url;
function openPopUp(url) {
	window.open(url, '', 'height=650,width=980,scrollbars=yes,left=0,top=0,status=yes');
}

function generateSno(className)
{
	var idx=1;
	$(className).each(function(){
		$(this).text(idx);
		idx++;
	});
}

function resetValues() {
		$('#retype-pass').val('');
		$('#new-pass').val('');
		$('#old-pass').val('');
		$('.password-error').hide();
}

function inboxloadmethod() {
	location.reload();
}

function configureTableData(dataset, dataKey){
	return dataset[dataKey].map(function(item, index) {
		item.serviceRequestDate = epochToYmd(item.serviceRequestDate);
		item.srNo = index+1;
		return item;
	})
}

function initiateTable(tableData, serviceGroup, dataKey){
	var clonedTableData = cloneDeep(tableData);
	if(serviceGroup && serviceGroup !== "all"){
		clonedTableData[dataKey] = clonedTableData[dataKey].filter(function(item) { return item.serviceGroup === serviceGroup});
	}
	
	var finalData = configureTableData(clonedTableData, dataKey);
	resetServicesCount();
	var datatable = $( "#bpa-home-table" ).DataTable();
	datatable.clear();
	datatable.rows.add(finalData);
	datatable.draw();
	$('#bpa-home-table tbody').on('click', 'tr', function (e) {
	    var data = datatable.row( this ).data();
	    e.stopImmediatePropagation();
	    openPopUp(window.origin+data.link);
	} );
}

function getServiceGroup(code){
	switch(code){
		case "all":
			return "all";
		case "edcr":
			return "Digit DCR";
		case "bpa":
			return "BPA";
		default: 
			return "all";
	}
}

function cloneDeep(obj){
	return JSON.parse(JSON.stringify(obj));
}

function fetchDataAndInitiateTable(url, dataKey){
	$.ajax({
        url: url,
//        dataType: 'text',
        type: 'POST',
        contentType: 'application/x-www-form-urlencoded',
        data: {
        	id: parseFloat($('#userId').val())
        },
        success: function( data, textStatus, jQxhr ){
        	var tableInitData = configureTableData(data, dataKey);
        	initiateTable(data, window.selectedService, dataKey);
        	//Initialize data in global variable
        	window.clickedServiceData = cloneDeep(data);
        	
        },
        error: function( jqXhr, textStatus, errorThrown ){
            console.log( errorThrown );
        }
    });
}

function resetServicesCount(){
	$.ajax({
        url: "/portal/rest/fetch/services/count/by-servicegroup",
        type: "POST",
        data: {
        	id: parseFloat($('#userId').val()),
        	serviceContextRoot: $('#serviceGroup').val()
        },
        cache : false,
        async: false,
        dataType: "json",
        success: function (response) {
        	$('#totalServicesAppliedSize').html('');
        	$('#totalServicesPendingSize').html('');
        	$('#totalServicesCompletedSize').html('');
        	$('#totalServicesAppliedSize').html(response.totalServices);
        	$('#totalServicesPendingSize').html(response.underScrutiny);
        	$('#totalServicesCompletedSize').html(response.completedServices);
        },
        error: function (response) {
            console.log("Error occurred while retrieving services!!!!!!!");
        }
    });
}

function epochToYmd(et) {
	// Return null if et already null
	if (!et) return null;
	// Return the same format if et is already a string (boundary case)
	if (typeof et === "string") return et;
	let date = new Date(et);
	let day = date.getDate() < 10 ? `0${date.getDate()}` : date.getDate();
	let month =
	date.getMonth() + 1 < 10 ? `0${date.getMonth() + 1}` : date.getMonth() + 1;
	// date = `${date.getFullYear()}-${month}-${day}`;
	var formatted_date = day + "-" + month+ "-" +date.getFullYear();
	return formatted_date;
}

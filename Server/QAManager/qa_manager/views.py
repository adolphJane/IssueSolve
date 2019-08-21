# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from rest_framework.decorators import api_view
from rest_framework import status
from rest_framework.response import Response

from .models import QA_API
from .serializers import QASerializer
import qa_manager.qa as qa

# Create your views here.

import sys
reload(sys)
sys.setdefaultencoding('utf-8')

@api_view(['POST'])
def get_result(request):
    print request.data
    if request.method == 'POST':
        serializer = QASerializer(data=request.data)
        if serializer.is_valid():
            issue = request.data.get('issue')
            if issue is None:
            	result = "question is null"
            else:
            	result = qa.qa(issue)
            return Response({'result':result},status=status.HTTP_200_OK)
        else:
            return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

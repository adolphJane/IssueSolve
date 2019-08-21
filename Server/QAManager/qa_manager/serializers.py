# -*- coding:utf-8 -*-
from rest_framework import serializers
from .models import QA_API


class QASerializer(serializers.ModelSerializer):
    class Meta:
        model = QA_API
        fields = ('id','issue')